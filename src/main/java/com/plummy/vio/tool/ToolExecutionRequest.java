package com.plummy.vio.tool;

public class ToolExecutionRequest {

    private final String name;
    private final ToolArguments arguments;

    public ToolExecutionRequest(String name, ToolArguments arguments) {
        this.name = name;
        this.arguments = arguments;
    }

    public String getName() {
        return name;
    }

    public ToolArguments getArguments() {
        return arguments;
    }
}
