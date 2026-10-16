package com.plummy.vio.tool;

import com.plummy.vio.Vio;
import com.plummy.vio.access.AccessLevel;
import com.plummy.vio.access.AccessManager;
import com.plummy.vio.registry.ToolRegistry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;
import java.util.concurrent.ExecutionException;

public class ServerThreadToolExecutor extends AbstractToolExecutor implements ToolExecutor {

    private final AccessManager accessManager;

    public ServerThreadToolExecutor(AccessManager accessManager) {
        this.accessManager = accessManager;
    }

    public SequencedMap<String, Tool.ExecutionResult> execute(MinecraftServer server, UUID playerId, SequencedMap<String, Tool.ExecutionRequest> requests) {
        if (!server.isRunning()) {
            return failAll(requests, "The server is shutting down.");
        }

        try {
            return server.submit(() -> executeOnServerThread(server, playerId, requests)).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return failAll(requests, "Interrupted because the server is shutting down.");
        } catch (ExecutionException e) {
            Vio.getLogger().error("Tool execution for {} failed", playerId, e);
            return failAll(requests, "Internal error while running the tools.");
        }
    }

    private SequencedMap<String, Tool.ExecutionResult> executeOnServerThread(MinecraftServer server, UUID playerId, SequencedMap<String, Tool.ExecutionRequest> requests) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);

        if (player == null) {
            return failAll(requests, "The player disconnected.");
        }

        AccessLevel accessLevel = accessManager.getAccessLevel(playerId);
        SequencedMap<String, Tool.ExecutionResult> results = new LinkedHashMap<>();

        for (Map.Entry<String, Tool.ExecutionRequest> entry : requests.entrySet()) {
            results.put(entry.getKey(), execute(player, accessLevel, entry.getValue()));
        }

        return results;
    }

    private Tool.ExecutionResult execute(ServerPlayer player, AccessLevel accessLevel, Tool.ExecutionRequest request) {
        Tool tool = ToolRegistry.get(request.getName());

        if (tool == null) {
            List<String> availableNames = getAvailableTools(accessLevel).stream().map(Tool::getName).toList();
            return Tool.ExecutionResult.invalidArguments("Unknown tool '" + request.getName() + "'. Available tools: " + availableNames);
        }

        if (!accessLevel.isAtLeast(tool.getAccessLevel())) {
            return Tool.ExecutionResult.denied("'" + tool.getName() + "' requires access level " + tool.getAccessLevel() + ", the player has " + accessLevel + ".");
        }

        try {
            return tool.execute(new Tool.Context(player), request.getArguments());
        } catch (Tool.ArgumentException e) {
            return Tool.ExecutionResult.invalidArguments(e.getMessage());
        } catch (RuntimeException e) {
            Vio.getLogger().error("Tool '{}' failed for {}", tool.getName(), player.getName().getString(), e);
            return Tool.ExecutionResult.failure("Internal error while running '" + tool.getName() + "'.");
        }
    }

    private static SequencedMap<String, Tool.ExecutionResult> failAll(SequencedMap<String, Tool.ExecutionRequest> requests, String reason) {
        SequencedMap<String, Tool.ExecutionResult> results = new LinkedHashMap<>();

        for (String callId : requests.keySet()) {
            results.put(callId, Tool.ExecutionResult.failure(reason));
        }

        return results;
    }
}
