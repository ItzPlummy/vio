package com.plummy.vio.tool;

import com.plummy.vio.tool.argument.Arguments;
import net.minecraft.server.MinecraftServer;

public class ServerToolExecutor extends AbstractToolExecutor<ServerTool> {

    private final MinecraftServer server;

    public ServerToolExecutor(MinecraftServer server) {
        super(ServerTool.class);
        this.server = server;
    }

    @Override
    public ExecutionResult execute(Requester requester, ExecutionRequest request) {
        if (!server.isRunning()) {
            return ExecutionResult.failure("Server is not running.");
        }

        return await(server.submit(() -> executeNow(requester, request)));
    }

    @Override
    protected ExecutionResult invoke(ServerTool tool, Requester requester, Arguments arguments) {
        return tool.execute(new ServerTool.Context(server, requester), arguments);
    }
}
