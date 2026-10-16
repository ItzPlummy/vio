package com.plummy.vio.agent;

import com.plummy.vio.Vio;
import com.plummy.vio.access.AccessLevel;
import com.plummy.vio.access.AccessManager;
import com.plummy.vio.config.SystemMessageProvider;
import com.plummy.vio.conversation.Conversation;
import com.plummy.vio.conversation.PlayerConversation;
import com.plummy.vio.message.*;
import com.plummy.vio.provider.Provider;
import com.plummy.vio.provider.ProviderException;
import com.plummy.vio.registry.ToolRegistry;
import com.plummy.vio.section.PlayerContextSection;
import com.plummy.vio.section.ServerContextSection;
import com.plummy.vio.storage.Storage;
import com.plummy.vio.tool.BasicToolExecutor;
import com.plummy.vio.tool.ServerToolExecutor;
import com.plummy.vio.tool.Tool;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;

public class VioAgent implements Agent {

    private static final int MAX_ROUNDS = 8;
    private static final int WORKER_THREADS = 4;
    private static final long PLAYER_LOOKUP_TIMEOUT_SECONDS = 5;

    private final Provider provider;
    private final Storage<UUID, Conversation> conversationStorage;
    private final AccessManager accessManager;
    private final BasicToolExecutor basicToolExecutor;
    private final ServerToolExecutor serverToolExecutor;
    private final SystemMessageProvider systemMessageProvider;
    private final AgentListener listener;

    private final ExecutorService worker = Executors.newFixedThreadPool(
            WORKER_THREADS,
            runnable -> {
                Thread thread = new Thread(runnable, "vio_agent");
                thread.setDaemon(true);
                return thread;
            }
    );

    private final Set<UUID> busyPlayers = ConcurrentHashMap.newKeySet();

    public VioAgent(Provider provider, Storage<UUID, Conversation> conversationStorage, AccessManager accessManager, BasicToolExecutor basicToolExecutor, ServerToolExecutor serverToolExecutor, SystemMessageProvider systemMessageProvider, AgentListener listener) {
        this.provider = provider;
        this.conversationStorage = conversationStorage;
        this.accessManager = accessManager;
        this.basicToolExecutor = basicToolExecutor;
        this.serverToolExecutor = serverToolExecutor;
        this.systemMessageProvider = systemMessageProvider;
        this.listener = listener;
    }

    @Override
    public SubmitResult submit(ServerPlayer player, String request, Instant time, String languageCode) {
        UUID playerId = player.getUUID();
        AccessLevel accessLevel = accessManager.getAccessLevel(playerId);

        if (accessLevel == AccessLevel.NONE) {
            return SubmitResult.DENIED;
        }

        if (!busyPlayers.add(playerId)) {
            return SubmitResult.BUSY;
        }

        MinecraftServer server = player.getServer();

        if (server == null) {
            busyPlayers.remove(playerId);
            return SubmitResult.UNAVAILABLE;
        }

        PlayerRequestMessage message = PlayerRequestMessage.of(
                request,
                PlayerContextSection.fromPlayer(player, accessLevel),
                ServerContextSection.fromServer(server),
                time
        );

        SystemMessage systemMessage = systemMessageProvider.getPrompt(languageCode);

        try {
            worker.execute(() -> process(server, playerId, accessLevel, systemMessage, message));
        } catch (RejectedExecutionException e) {
            busyPlayers.remove(playerId);
            return SubmitResult.UNAVAILABLE;
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
            conversationStorage.delete(playerId);
        } finally {
            busyPlayers.remove(playerId);
        }

        return true;
    }

    @Override
    public void shutdown() {
        worker.shutdownNow();
    }

    private void process(MinecraftServer server, UUID playerId, AccessLevel accessLevel, SystemMessage systemMessage, PlayerRequestMessage message) {
        Conversation conversation = null;

        try {
            conversation = conversationStorage.get(playerId);

            if (conversation == null) {
                conversation = new PlayerConversation(playerId);
            }

            conversation.addMessage(message);

            for (int round = 0; round < MAX_ROUNDS; round++) {
                ConversationMessage response = provider.request(accessLevel, systemMessage, conversation);
                conversation.addMessage(response);

                if (response instanceof AgentResponseMessage agentResponse) {
                    notify(server, playerId, (listener, player) -> listener.onResponse(player, agentResponse));
                    return;
                }

                if (!(response instanceof ToolRequestMessage toolRequest)) {
                    throw new IllegalStateException("Unexpected provider response: " + response.getType());
                }

                notify(server, playerId, (listener, player) -> listener.onActivity(player, toolRequest));

                conversation.addMessage(ToolResponseMessage.of(executeTools(accessLevel, toolRequest.getToolExecutionRequests())));

                if (findPlayer(server, playerId) == null) {
                    return;
                }
            }

            notify(server, playerId, (listener, player) -> listener.onError(player, error("max_rounds", MAX_ROUNDS)));
        } catch (ProviderException e) {
            Vio.getLogger().error("Agent request for {} failed", playerId, e);
            notify(server, playerId, (listener, player) -> listener.onError(player, error("provider")));
        } catch (RuntimeException e) {
            Vio.getLogger().error("Agent request for {} failed", playerId, e);
            notify(server, playerId, (listener, player) -> listener.onError(player, error("internal")));
        } finally {
            if (conversation != null) {
                save(server, playerId, conversation);
            }

            busyPlayers.remove(playerId);
        }
    }

    private void save(MinecraftServer server, UUID playerId, Conversation conversation) {
        try {
            conversationStorage.save(conversation);
        } catch (RuntimeException e) {
            Vio.getLogger().error("Failed to save conversation of {}", playerId, e);
            notify(server, playerId, (listener, player) -> listener.onError(player, error("save")));
        }
    }

    // Translated on the client, in the player's language.
    private static Component error(String name, Object... args) {
        return Component.translatable("vio.agent.error." + name, args);
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

    private SequencedMap<String, Tool.ExecutionResult> executeTools(AccessLevel accessLevel, SequencedMap<String, Tool.ExecutionRequest> tools) {
        SequencedMap<String, Tool.ExecutionResult> results = new LinkedHashMap<>();

        for (Map.Entry<String, Tool.ExecutionRequest> entry : tools.entrySet()) {
            Tool tool = ToolRegistry.get(entry.getValue().getName());

            Tool.ExecutionResult result;

            if (tool == null) {
                result = Tool.ExecutionResult.invalidArguments("Unknown tool: %s.".formatted(entry.getValue().getName()));
            } else {
                result = switch (tool.getType()) {
                    case BASIC -> basicToolExecutor.execute(accessLevel, entry.getValue());
                    case SERVER -> serverToolExecutor.execute(accessLevel, entry.getValue());
                };
            }

            results.put(entry.getKey(), result);
        }

        return results;
    }
}
