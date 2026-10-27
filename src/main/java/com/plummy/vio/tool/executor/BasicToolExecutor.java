package com.plummy.vio.tool.executor;

import com.plummy.vio.tool.BasicTool;
import com.plummy.vio.tool.requester.ToolRequester;
import com.plummy.vio.tool.value.argument.Arguments;

public class BasicToolExecutor extends AbstractToolExecutor<BasicTool> {

    @Override
    protected ExecutionResult call(ToolRequester requester, BasicTool tool, Arguments arguments) {
        return tool.execute(requester, arguments);
    }
}
