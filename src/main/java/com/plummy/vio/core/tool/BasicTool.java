package com.plummy.vio.core.tool;

import com.plummy.vio.core.tool.argument.Arguments;

public interface BasicTool extends Tool {

    ExecutionResult execute(Requester requester, Arguments arguments);

    default ToolType getType() {
        return ToolType.BASIC;
    }
}
