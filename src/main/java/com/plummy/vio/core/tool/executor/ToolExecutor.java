package com.plummy.vio.core.tool.executor;

import com.plummy.vio.core.tool.Tool;
import com.plummy.vio.core.tool.requester.ToolRequester;
import com.plummy.vio.core.tool.value.argument.Arguments;

public interface ToolExecutor<T extends Tool> {

    ExecutionResult execute(ToolRequester requester, T tool, Arguments arguments);
}
