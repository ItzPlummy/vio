package com.plummy.vio.tool;

public interface BasicTool extends Tool {

    ExecutionResult execute(Arguments arguments);

    default ToolType getType() {
        return ToolType.BASIC;
    }
}
