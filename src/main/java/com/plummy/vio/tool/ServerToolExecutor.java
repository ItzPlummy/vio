package com.plummy.vio.tool;

import com.plummy.vio.access.AccessLevel;
import net.minecraft.server.MinecraftServer;

public class ServerToolExecutor extends AbstractToolExecutor<ServerTool> {

    private final MinecraftServer server;

    public ServerToolExecutor(MinecraftServer server) {
        super(ServerTool.class);
        this.server = server;
    }

    @Override
    public Tool.ExecutionResult execute(AccessLevel accessLevel, Tool.ExecutionRequest request) {
        if (!server.isRunning()) {
            return Tool.ExecutionResult.failure("Server is not running.");
        }

        return await(server.submit(() -> executeNow(accessLevel, request)));
    }

    @Override
    protected Tool.ExecutionResult invoke(ServerTool tool, Tool.Arguments arguments) {
        return tool.execute(new ServerTool.Context(server), arguments);
    }
}
