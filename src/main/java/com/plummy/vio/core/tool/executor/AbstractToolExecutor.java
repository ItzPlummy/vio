package com.plummy.vio.core.tool.executor;

import com.plummy.vio.core.tool.Tool;
import com.plummy.vio.core.tool.argument.Arguments;
import com.plummy.vio.core.user.User;

import java.util.function.Supplier;

abstract class AbstractToolExecutor<T extends Tool> {

    public ExecutionResult execute(T tool, User user, Arguments arguments) {
        if (!user.getAccessLevel().isAtLeast(tool.getAccessLevel())) {
            return ExecutionResult.denied("Tool %s requires access level %s, user has %s".formatted(tool.getName(), tool.getAccessLevel().getValue(), user.getAccessLevel().getValue()));
        }

        return schedule(
                () -> {
                    try {
                        return call(tool, user, arguments);
                    } catch (ExecutionException exception) {
                        return exception.getResult();
                    } catch (RuntimeException exception) {
                        return ExecutionResult.failure("An internal error occurred while executing tool %s".formatted(tool.getName()));
                    }
                }
        );
    }

    protected abstract ExecutionResult call(T tool, User user, Arguments arguments);

    protected abstract ExecutionResult schedule(Supplier<ExecutionResult> task);
}
