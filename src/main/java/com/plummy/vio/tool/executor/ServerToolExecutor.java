package com.plummy.vio.tool.executor;

import com.plummy.vio.Vio;
import com.plummy.vio.tool.ServerTool;
import com.plummy.vio.tool.requester.ToolRequester;
import com.plummy.vio.tool.value.argument.Arguments;
import net.minecraft.server.MinecraftServer;

import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

public class ServerToolExecutor extends AbstractToolExecutor<ServerTool> {

    private final MinecraftServer server;

    public ServerToolExecutor(MinecraftServer server) {
        this.server = server;
    }

    @Override
    protected ExecutionResult call(ToolRequester requester, ServerTool tool, Arguments arguments) {
        return tool.execute(new ServerTool.Context(server, requester), arguments);
    }

    @Override
    protected ExecutionResult schedule(Supplier<ExecutionResult> task) {
        if (!server.isRunning()) {
            return ExecutionResult.failure("Server isn't running");
        }

        try {
            return server.submit(task).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ExecutionResult.failure("Execution interrupted");
        } catch (ExecutionException e) {
            Vio.getLogger().error("Failed to execute a tool on the server thread", e);
            return ExecutionResult.failure("Internal error while executing the tool");
        }
    }
}
