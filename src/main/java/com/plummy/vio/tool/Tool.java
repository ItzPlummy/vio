package com.plummy.vio.tool;

import java.util.SequencedMap;

public interface Tool {

    String getName();

    String getDescription();

    SequencedMap<String, ToolArgumentDefinition> getArgumentSchema();

    ToolExecutionResult execute(ToolContext context, ToolArguments arguments);
}
