package com.plummy.vio.tool.executor;

import com.plummy.vio.tool.Tool;
import com.plummy.vio.tool.requester.ToolRequester;
import com.plummy.vio.tool.value.argument.Arguments;

public interface ToolExecutor<T extends Tool> {

    ExecutionResult execute(ToolRequester requester, T tool, Arguments arguments);
}
