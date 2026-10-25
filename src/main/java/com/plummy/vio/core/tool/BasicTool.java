package com.plummy.vio.core.tool;

import com.plummy.vio.core.tool.executor.ExecutionResult;
import com.plummy.vio.core.tool.requester.ToolRequester;
import com.plummy.vio.core.tool.value.argument.Arguments;

public interface BasicTool extends Tool {

    ExecutionResult execute(ToolRequester requester, Arguments arguments);
}
