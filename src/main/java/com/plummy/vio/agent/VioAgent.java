package com.plummy.vio.agent;

import com.plummy.vio.Vio;
import com.plummy.vio.conversation.Conversation;
import com.plummy.vio.conversation.PlayerConversation;
import com.plummy.vio.message.*;
import com.plummy.vio.provider.AgentProvider;
import com.plummy.vio.provider.AgentProviderException;
import com.plummy.vio.section.PlayerContextSection;
import com.plummy.vio.section.ServerContextSection;
import com.plummy.vio.storage.Storage;
import com.plummy.vio.tool.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class VioAgent implements Agent {

    private static final int MAX_ROUNDS = 8;
    private static final int WORKER_THREADS = 2;
    private static final long PLAYER_LOOKUP_TIMEOUT_SECONDS = 5;

    private final AgentProvider provider;
    private final Storage<UUID, Conversation> storage;
    private final ToolExecutor toolExecutor;
    private final Supplier<SystemMessage> systemMessage;
    private final Collection<Tool> tools;
    private final AgentListener listener;

    private final ExecutorService worker = Executors.newFixedThreadPool(WORKER_THREADS, runnable -> {
        Thread thread = new Thread(runnable, "Vio Agent");
        thread.setDaemon(true);
        return thread;
    });

    private final Set<UUID> busyPlayers = ConcurrentHashMap.newKeySet();

    public VioAgent(AgentProvider provider, Storage<UUID, Conversation> storage, ToolExecutor toolExecutor, Supplier<SystemMessage> systemMessage, Collection<Tool> tools, AgentListener listener) {
        this.provider = provider;
        this.storage = storage;
        this.toolExecutor = toolExecutor;
        this.systemMessage = systemMessage;
        this.tools = tools;
        this.listener = listener;
    }

    @Override
    public SubmitResult submit(ServerPlayer player, String request) {
        UUID playerId = player.getUUID();

        if (!busyPlayers.add(playerId)) {
            return SubmitResult.BUSY;
        }

        MinecraftServer server = player.getServer();

        if (server == null) {
            busyPlayers.remove(playerId);
            return SubmitResult.BUSY;
        }

        PlayerRequestMessage message = PlayerRequestMessage.of(
                request,
                PlayerContextSection.fromPlayer(player),
                ServerContextSection.fromServer(server)
        );

        try {
            worker.execute(() -> process(server, playerId, message, tools));
        } catch (RejectedExecutionException e) {
            busyPlayers.remove(playerId);
            return SubmitResult.BUSY;
        }

        return SubmitResult.ACCEPTED;
    }

    @Override
    public boolean isBusy(UUID playerId) {
        return busyPlayers.contains(playerId);
    }

    @Override
    public boolean reset(UUID playerId) {
        if (!busyPlayers.add(playerId)) {
            return false;
        }

        try {
            storage.delete(playerId);
        } finally {
            busyPlayers.remove(playerId);
        }

        return true;
    }

    @Override
    public void shutdown() {
        worker.shutdownNow();
    }

    private void process(MinecraftServer server, UUID playerId, PlayerRequestMessage message, Collection<Tool> tools) {
        Conversation conversation = null;

        try {
            conversation = storage.get(playerId);

            if (conversation == null) {
                conversation = new PlayerConversation(playerId);
            }

            conversation.addMessage(message);

            SystemMessage currentSystemMessage = systemMessage.get();

            for (int round = 0; round < MAX_ROUNDS; round++) {
                ConversationMessage response = provider.request(currentSystemMessage, conversation, tools);
                conversation.addMessage(response);

                if (response instanceof AgentResponseMessage agentResponse) {
                    notify(server, playerId, (listener, player) -> listener.onResponse(player, agentResponse));
                    return;
                }

                if (!(response instanceof ToolRequestMessage toolRequest)) {
                    throw new IllegalStateException("Unexpected provider response: " + response.getType());
                }

                notify(server, playerId, (listener, player) -> listener.onActivity(player, toolRequest.getActivityDescription()));
                conversation.addMessage(executeTools(server, playerId, toolRequest));

                if (findPlayer(server, playerId) == null) {
                    return;
                }
            }

            notify(server, playerId, (listener, player) -> listener.onError(player, "Stopped after " + MAX_ROUNDS + " steps without a final answer."));
        } catch (AgentProviderException e) {
            Vio.getLogger().error("Agent request for {} failed", playerId, e);
            notify(server, playerId, (listener, player) -> listener.onError(player, "The AI provider request failed. Details are in the server log."));
        } catch (RuntimeException e) {
            Vio.getLogger().error("Agent request for {} failed", playerId, e);
            notify(server, playerId, (listener, player) -> listener.onError(player, "Something went wrong. Details are in the server log."));
        } finally {
            // Every round is complete at this point (a tool request is always followed by its response), so saving is always safe.
            if (conversation != null) {
                save(server, playerId, conversation);
            }

            busyPlayers.remove(playerId);
        }
    }

    private ToolResponseMessage executeTools(MinecraftServer server, UUID playerId, ToolRequestMessage toolRequest) {
        ServerPlayer player = findPlayer(server, playerId);

        SequencedMap<String, CompletableFuture<ToolExecutionResult>> futures = new LinkedHashMap<>();

        for (Map.Entry<String, ToolExecutionRequest> entry : toolRequest.getToolExecutionRequests().entrySet()) {
            futures.put(entry.getKey(), player == null
                    ? CompletableFuture.completedFuture(ToolExecutionResult.failure("The player disconnected."))
                    : toolExecutor.execute(entry.getValue(), new ToolContext(player)));
        }

        SequencedMap<String, ToolExecutionResult> results = new LinkedHashMap<>();

        for (Map.Entry<String, CompletableFuture<ToolExecutionResult>> entry : futures.entrySet()) {
            results.put(entry.getKey(), entry.getValue().join());
        }

        return new ToolResponseMessage(results);
    }

    private void save(MinecraftServer server, UUID playerId, Conversation conversation) {
        try {
            storage.save(conversation);
        } catch (RuntimeException e) {
            Vio.getLogger().error("Failed to save conversation of {}", playerId, e);
            notify(server, playerId, (listener, player) -> listener.onError(player, "Failed to save the conversation. Details are in the server log."));
        }
    }

    private static ServerPlayer findPlayer(MinecraftServer server, UUID playerId) {
        if (!server.isRunning()) {
            return null;
        }

        try {
            return server.submit(() -> server.getPlayerList().getPlayer(playerId)).get(PLAYER_LOOKUP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (ExecutionException | TimeoutException e) {
            return null;
        }
    }

    private void notify(MinecraftServer server, UUID playerId, BiConsumer<AgentListener, ServerPlayer> callback) {
        if (!server.isRunning()) {
            return;
        }

        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);

            if (player != null) {
                callback.accept(listener, player);
            }
        });
    }
}
