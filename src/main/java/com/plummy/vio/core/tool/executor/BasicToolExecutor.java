package com.plummy.vio.core.tool.executor;

import com.plummy.vio.core.tool.BasicTool;
import com.plummy.vio.core.tool.argument.Arguments;
import com.plummy.vio.core.user.User;

import java.util.function.Supplier;

public final class BasicToolExecutor extends AbstractToolExecutor<BasicTool> implements ToolExecutor<BasicTool> {

    @Override
    protected ExecutionResult call(BasicTool tool, User user, Arguments arguments) {
        return tool.execute(user, arguments);
    }

    @Override
    protected ExecutionResult schedule(Supplier<ExecutionResult> task) {
        return task.get();
    }
}
