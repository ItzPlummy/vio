package com.plummy.vio.tool.executor;

import com.plummy.vio.tool.value.argument.Arguments;

public class ExecutionRequest {

    private final String name;
    private final Arguments arguments;

    public ExecutionRequest(String name, Arguments arguments) {
        this.name = name;
        this.arguments = arguments;
    }

    public String getName() {
        return name;
    }

    public Arguments getArguments() {
        return arguments;
    }
}
