package com.plummy.vio.agent;

import com.plummy.vio.Vio;
import com.plummy.vio.access.AccessLevel;
import com.plummy.vio.access.AccessManager;
import com.plummy.vio.config.AgentLimits;
import com.plummy.vio.config.Instructions;
import com.plummy.vio.conversation.PlayerConversation;
import com.plummy.vio.listener.AgentListener;
import com.plummy.vio.message.*;
import com.plummy.vio.provider.Provider;
import com.plummy.vio.provider.ProviderException;
import com.plummy.vio.script.ScriptManager;
import com.plummy.vio.storage.Storage;
import com.plummy.vio.tool.BasicTool;
import com.plummy.vio.tool.ServerTool;
import com.plummy.vio.tool.Tool;
import com.plummy.vio.tool.ToolRegistry;
import com.plummy.vio.tool.catalog.script.ScriptTools;
import com.plummy.vio.tool.executor.BasicToolExecutor;
import com.plummy.vio.tool.executor.ExecutionRequest;
import com.plummy.vio.tool.executor.ExecutionResult;
import com.plummy.vio.tool.executor.ServerToolExecutor;
import com.plummy.vio.tool.requester.PlayerToolRequester;
import com.plummy.vio.tool.requester.ToolRequester;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;

public class VioAgent implements Agent {

    private final Provider provider;
    private final Storage<UUID, PlayerConversation> conversationStorage;
    private final AccessManager accessManager;
    private final BasicToolExecutor basicToolExecutor;
    private final ServerToolExecutor serverToolExecutor;
    private final Instructions instructions;
    private final List<AgentListener> listeners;

    private final ExecutorService worker = Executors.newFixedThreadPool(
            AgentLimits.WORKER_THREADS,
            runnable -> {
                Thread thread = new Thread(runnable, "vio_agent");
                thread.setDaemon(true);
                return thread;
            }
    );

    private final Set<UUID> busyPlayers = ConcurrentHashMap.newKeySet();

    public VioAgent(Provider provider, Storage<UUID, PlayerConversation> conversationStorage, AccessManager accessManager, BasicToolExecutor basicToolExecutor, ServerToolExecutor serverToolExecutor, Instructions instructions, List<AgentListener> listeners) {
        this.provider = provider;
        this.conversationStorage = conversationStorage;
        this.accessManager = accessManager;
        this.basicToolExecutor = basicToolExecutor;
        this.serverToolExecutor = serverToolExecutor;
        this.instructions = instructions;
        this.listeners = List.copyOf(listeners);
    }

    @Override
    public SubmitResult submit(ServerPlayer player, String request, Instant time, boolean voice) {
        UUID playerId = player.getUUID();
        AccessLevel accessLevel = accessManager.getPlayerAccessLevel(playerId);

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

        ScriptManager scriptManager = Vio.getScriptManager();
        boolean scripts = accessLevel == AccessLevel.FULL && ScriptTools.isAvailable() && scriptManager != null;

        PlayerRequestMessage message = PlayerRequestMessage.of(
                request,
                player,
                server,
                accessLevel,
                scripts ? PlayerRequestMessage.ScriptInfo.of(scriptManager) : null,
                voice,
                time
        );

        try {
            worker.execute(() -> process(server, playerId, accessLevel, message));
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

    private void process(MinecraftServer server, UUID playerId, AccessLevel accessLevel, PlayerRequestMessage message) {
        PlayerConversation conversation = null;

        try {
            conversation = conversationStorage.get(playerId);

            if (conversation == null) {
                conversation = new PlayerConversation(playerId);
            }

            conversation.addMessage(message);

            for (int round = 0; round < AgentLimits.MAX_ROUNDS; round++) {
                Message response = provider.request(accessLevel, instructions.getText(), conversation);
                conversation.addMessage(response);

                if (response instanceof AgentResponseMessage agentResponse) {
                    notify(server, playerId, (listener, player) -> listener.onResponse(player, agentResponse));
                    return;
                }

                if (!(response instanceof ToolRequestMessage toolRequest)) {
                    throw new IllegalStateException("Unexpected provider response: %s".formatted(response.getClass().getSimpleName()));
                }

                notify(server, playerId, (listener, player) -> listener.onActivity(player, toolRequest));

                conversation.addMessage(new ToolResponseMessage(executeTools(new PlayerToolRequester(playerId, accessLevel), toolRequest.getToolExecutionRequests())));

                if (findPlayer(server, playerId) == null) {
                    return;
                }
            }

            notify(server, playerId, (listener, player) -> listener.onError(player, error("max_rounds", AgentLimits.MAX_ROUNDS)));
        } catch (ProviderException e) {
            Vio.getLogger().error("Failed to process the request of %s".formatted(playerId), e);
            notify(server, playerId, (listener, player) -> listener.onError(player, error("provider")));
        } catch (RuntimeException e) {
            Vio.getLogger().error("Failed to process the request of %s".formatted(playerId), e);
            notify(server, playerId, (listener, player) -> listener.onError(player, error("internal")));
        } finally {
            if (conversation != null) {
                save(server, playerId, conversation);
            }

            busyPlayers.remove(playerId);
        }
    }

    private void save(MinecraftServer server, UUID playerId, PlayerConversation conversation) {
        try {
            conversationStorage.save(conversation);
        } catch (RuntimeException e) {
            Vio.getLogger().error("Failed to save the conversation of %s".formatted(playerId), e);
            notify(server, playerId, (listener, player) -> listener.onError(player, error("save")));
        }
    }

    // Translated on the client, in the player's language.
    private static Component error(String name, Object... args) {
        return Component.translatable("vio.agent.error.%s".formatted(name), args);
    }

    private static ServerPlayer findPlayer(MinecraftServer server, UUID playerId) {
        if (!server.isRunning()) {
            return null;
        }

        try {
            return server.submit(() -> server.getPlayerList().getPlayer(playerId)).get(AgentLimits.PLAYER_LOOKUP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
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
                listeners.forEach(listener -> callback.accept(listener, player));
            }
        });
    }

    private SequencedMap<String, ExecutionResult> executeTools(ToolRequester requester, SequencedMap<String, ExecutionRequest> requests) {
        SequencedMap<String, ExecutionResult> results = new LinkedHashMap<>();

        for (Map.Entry<String, ExecutionRequest> entry : requests.entrySet()) {
            results.put(entry.getKey(), executeTool(requester, entry.getValue()));
        }

        return results;
    }

    private ExecutionResult executeTool(ToolRequester requester, ExecutionRequest request) {
        Tool tool = ToolRegistry.get(request.getName());

        if (tool == null || !tool.isAvailable()) {
            return ExecutionResult.invalidArguments("Unknown tool: %s".formatted(request.getName()));
        }

        return switch (tool) {
            case BasicTool basicTool -> basicToolExecutor.execute(requester, basicTool, request.getArguments());
            case ServerTool serverTool -> serverToolExecutor.execute(requester, serverTool, request.getArguments());
            default -> ExecutionResult.invalidArguments("Unknown tool type: %s".formatted(tool.getClass().getName()));
        };
    }
}
