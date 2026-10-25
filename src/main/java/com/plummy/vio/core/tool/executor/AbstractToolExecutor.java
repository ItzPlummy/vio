package com.plummy.vio.core.tool.executor;

import com.plummy.vio.Vio;
import com.plummy.vio.core.tool.Tool;
import com.plummy.vio.core.tool.requester.ToolRequester;
import com.plummy.vio.core.tool.value.argument.Argument;
import com.plummy.vio.core.tool.value.argument.Arguments;

import java.util.function.Supplier;

public abstract class AbstractToolExecutor<T extends Tool> implements ToolExecutor<T> {

    @Override
    public ExecutionResult execute(ToolRequester requester, T tool, Arguments arguments) {
        if (!requester.getAccessLevel().isAtLeast(tool.getAccessLevel())) {
            return ExecutionResult.denied("Tool %s requires access level %s, the player has %s".formatted(tool.getName(), tool.getAccessLevel(), requester.getAccessLevel()));
        }

        return schedule(
                () -> {
                    try {
                        return call(requester, tool, arguments);
                    } catch (Argument.ParseException e) {
                        return ExecutionResult.invalidArguments(e.getMessage());
                    } catch (ExecutionResultException e) {
                        return e.getResult();
                    } catch (RuntimeException e) {
                        Vio.getLogger().error("Failed to execute tool %s".formatted(tool.getName()), e);
                        return ExecutionResult.failure("Internal error while executing the tool");
                    }
                }
        );
    }

    protected abstract ExecutionResult call(ToolRequester requester, T tool, Arguments arguments);

    protected ExecutionResult schedule(Supplier<ExecutionResult> task) {
        return task.get();
    }
}
