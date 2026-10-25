package com.plummy.vio.core.tool.executor;

import com.plummy.vio.core.tool.BasicTool;
import com.plummy.vio.core.tool.requester.ToolRequester;
import com.plummy.vio.core.tool.value.argument.Arguments;

public class BasicToolExecutor extends AbstractToolExecutor<BasicTool> {

    @Override
    protected ExecutionResult call(ToolRequester requester, BasicTool tool, Arguments arguments) {
        return tool.execute(requester, arguments);
    }
}
