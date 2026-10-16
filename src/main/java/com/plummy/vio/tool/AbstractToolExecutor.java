package com.plummy.vio.tool;

import com.plummy.vio.Vio;
import com.plummy.vio.access.AccessLevel;
import com.plummy.vio.registry.ToolRegistry;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public abstract class AbstractToolExecutor<T extends Tool> implements ToolExecutor {

    private final Class<T> toolClass;

    protected AbstractToolExecutor(Class<T> toolClass) {
        this.toolClass = toolClass;
    }

    protected abstract Tool.ExecutionResult invoke(T tool, Tool.Arguments arguments);

    protected Tool.ExecutionResult executeNow(AccessLevel accessLevel, Tool.ExecutionRequest request) {
        Tool registered = ToolRegistry.get(request.getName());

        if (!toolClass.isInstance(registered)) {
            return Tool.ExecutionResult.invalidArguments("Unknown tool: %s.".formatted(request.getName()));
        }

        T tool = toolClass.cast(registered);

        if (!accessLevel.isAtLeast(tool.getAccessLevel())) {
            return Tool.ExecutionResult.denied("Tool '%s' requires access level '%s', the player has '%s'.".formatted(tool.getName(), tool.getAccessLevel(), accessLevel));
        }

        try {
            return invoke(tool, request.getArguments());
        } catch (Tool.ArgumentException e) {
            return Tool.ExecutionResult.invalidArguments(e.getMessage());
        } catch (RuntimeException e) {
            Vio.getLogger().error("Tool {} failed", tool.getName(), e);
            return Tool.ExecutionResult.failure("Internal error while executing the tool.");
        }
    }

    protected static Tool.ExecutionResult await(Future<Tool.ExecutionResult> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Tool.ExecutionResult.failure("Execution interrupted.");
        } catch (ExecutionException e) {
            Vio.getLogger().error("Tool execution failed", e);
            return Tool.ExecutionResult.failure("Internal error while executing the tools.");
        }
    }
}
