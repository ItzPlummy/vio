package com.plummy.vio.tool;

import com.plummy.vio.access.AccessLevel;
import com.plummy.vio.registry.ToolRegistry;
import net.minecraft.server.MinecraftServer;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class ServerToolExecutor extends AbstractToolExecutor<ServerTool> {

    private final MinecraftServer server;

    public ServerToolExecutor(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public List<ServerTool> getAvailableTools(AccessLevel accessLevel) {
        return ToolRegistry.getAll(ServerTool.class).stream().toList();
    }

    @Override
    public Tool.ExecutionResult execute(AccessLevel accessLevel, Tool.ExecutionRequest request) {
        if (!server.isRunning()) {
            return Tool.ExecutionResult.failure("Server is not running.");
        }

        try {
            return server.submit(
                    () -> {
                        ServerTool tool = ToolRegistry.get(request.getName(), ServerTool.class);

                        if (tool == null) {
                            return Tool.ExecutionResult.invalidArguments("Unknown tool: %s.".formatted(request.getName()));
                        }

                        if (!accessLevel.isAtLeast(tool.getAccessLevel())) {
                            return Tool.ExecutionResult.denied("Tool '%s' requires access level '%s', the player has '%s'.".formatted(tool.getName(), tool.getAccessLevel(), accessLevel));
                        }

                        try {
                            return tool.execute(new ServerTool.Context(server), request.getArguments());
                        } catch (Tool.ArgumentException e) {
                            return Tool.ExecutionResult.invalidArguments(e.getMessage());
                        } catch (RuntimeException e) {
                            return Tool.ExecutionResult.failure("Internal error while executing the tool.");
                        }
                    }
                    ).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Tool.ExecutionResult.failure("Server is shutting down.");
        } catch (ExecutionException e) {
            return Tool.ExecutionResult.failure("Internal error while executing the tools.");
        }
    }
}
