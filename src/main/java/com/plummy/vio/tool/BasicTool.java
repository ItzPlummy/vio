package com.plummy.vio.tool;

import com.plummy.vio.tool.executor.ExecutionResult;
import com.plummy.vio.tool.requester.ToolRequester;
import com.plummy.vio.tool.value.argument.Arguments;

public interface BasicTool extends Tool {

    ExecutionResult execute(ToolRequester requester, Arguments arguments);
}
