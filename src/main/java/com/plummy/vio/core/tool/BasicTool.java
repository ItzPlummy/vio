package com.plummy.vio.core.tool;

import com.plummy.vio.core.tool.argument.Arguments;
import com.plummy.vio.core.user.User;
import com.plummy.vio.core.tool.executor.ExecutionResult;

public non-sealed interface BasicTool extends Tool {

    ExecutionResult execute(User user, Arguments arguments);
}
