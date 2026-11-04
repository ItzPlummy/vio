package com.plummy.vio.core.tool.executor;

import com.plummy.vio.core.tool.Tool;
import com.plummy.vio.core.tool.argument.Arguments;
import com.plummy.vio.core.user.User;

public sealed interface ToolExecutor<T extends Tool> permits BasicToolExecutor, ServerToolExecutor, ToolExecutorPolicy {

    ExecutionResult execute(T tool, User user, Arguments arguments);
}
