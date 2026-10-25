package com.plummy.vio.core.tool;

import com.plummy.vio.Vio;
import com.plummy.vio.core.tool.argument.Argument;
import com.plummy.vio.core.tool.argument.Arguments;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public abstract class AbstractToolExecutor<T extends Tool> implements ToolExecutor {

    private final Class<T> toolClass;

    protected AbstractToolExecutor(Class<T> toolClass) {
        this.toolClass = toolClass;
    }

    protected abstract ExecutionResult invoke(T tool, Requester requester, Arguments arguments);

    protected ExecutionResult executeNow(Requester requester, ExecutionRequest request) {
        Tool registered = ToolRegistry.get(request.getName());

        if (!toolClass.isInstance(registered) || !registered.isAvailable()) {
            return ExecutionResult.invalidArguments("Unknown tool: %s.".formatted(request.getName()));
        }

        T tool = toolClass.cast(registered);

        if (!requester.getAccessLevel().isAtLeast(tool.getAccessLevel())) {
            return ExecutionResult.denied("Tool '%s' requires access level '%s', the player has '%s'.".formatted(tool.getName(), tool.getAccessLevel(), requester.getAccessLevel()));
        }

        try {
            return invoke(tool, requester, request.getArguments());
        } catch (Argument.ParseException e) {
            return ExecutionResult.invalidArguments(e.getMessage());
        } catch (ResultException e) {
            return e.getResult();
        } catch (RuntimeException e) {
            Vio.getLogger().error("Tool {} failed", tool.getName(), e);
            return ExecutionResult.failure("Internal error while executing the tool.");
        }
    }

    protected static ExecutionResult await(Future<ExecutionResult> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ExecutionResult.failure("Execution interrupted.");
        } catch (ExecutionException e) {
            Vio.getLogger().error("Tool execution failed", e);
            return ExecutionResult.failure("Internal error while executing the tools.");
        }
    }
}
