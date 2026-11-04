package com.plummy.vio.core.tool.executor;

import com.plummy.vio.core.tool.ServerTool;
import com.plummy.vio.core.tool.argument.Arguments;
import com.plummy.vio.core.user.User;
import net.minecraft.server.MinecraftServer;

import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

public final class ServerToolExecutor extends AbstractToolExecutor<ServerTool> implements ToolExecutor<ServerTool> {

    private final MinecraftServer server;

    public ServerToolExecutor(MinecraftServer server) {
        this.server = server;
    }

    @Override
    protected ExecutionResult call(ServerTool tool, User user, Arguments arguments) {
        return tool.execute(user, arguments, new ServerTool.Context(server));
    }

    @Override
    protected ExecutionResult schedule(Supplier<ExecutionResult> task) {
        if (!server.isRunning()) {
            return ExecutionResult.failure("Server is not running");
        }

        try {
            return server.submit(task).get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return ExecutionResult.failure("Execution interrupted");
        } catch (ExecutionException exception) {
            return ExecutionResult.failure("An internal error occurred while executing the tool");
        }
    }
}
