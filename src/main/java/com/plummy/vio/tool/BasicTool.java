package com.plummy.vio.tool;

import com.plummy.vio.tool.argument.Arguments;

public interface BasicTool extends Tool {

    ExecutionResult execute(Arguments arguments);

    default ToolType getType() {
        return ToolType.BASIC;
    }
}
