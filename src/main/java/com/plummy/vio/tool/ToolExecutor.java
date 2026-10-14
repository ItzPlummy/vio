package com.plummy.vio.tool;

import com.plummy.vio.registry.ToolRegistry;
import net.minecraft.server.MinecraftServer;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ToolExecutor {

    private static final long TIMEOUT_SECONDS = 10;

    public CompletableFuture<ToolExecutionResult> execute(ToolExecutionRequest toolExecutionRequest, ToolContext context) {
        Tool tool = ToolRegistry.get(toolExecutionRequest.getName());

        if (tool == null) {
            return CompletableFuture.completedFuture(
                    ToolExecutionResult.invalidArguments("Unknown tool '" + toolExecutionRequest.getName() + "'. Available tools: " + ToolRegistry.getNames())
            );
        }

        MinecraftServer server = context.getServer();

        if (!server.isRunning()) {
            return CompletableFuture.completedFuture(ToolExecutionResult.failure("The server is shutting down."));
        }

        return server.submit(() -> tool.execute(context, toolExecutionRequest.getArguments())).orTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS).exceptionally(throwable -> handleException(tool, throwable));
    }

    private static ToolExecutionResult handleException(Tool tool, Throwable throwable) {
        Throwable cause = throwable instanceof CompletionException && throwable.getCause() != null ? throwable.getCause() : throwable;

        if (cause instanceof ToolArgumentException) {
            return ToolExecutionResult.invalidArguments(cause.getMessage());
        }

        if (cause instanceof TimeoutException) {
            return ToolExecutionResult.failure("'" + tool.getName() + "' did not finish within " + TIMEOUT_SECONDS + " seconds.");
        }

        return ToolExecutionResult.failure("Internal error while running '" + tool.getName() + "'.");
    }
}
