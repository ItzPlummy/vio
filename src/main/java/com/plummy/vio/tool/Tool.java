package com.plummy.vio.tool;

import com.plummy.vio.access.AccessLevel;
import com.plummy.vio.tool.argument.*;

import java.util.LinkedHashMap;
import java.util.SequencedMap;

public interface Tool {

    ToolType getType();

    String getName();

    String getDescription();

    SequencedMap<String, ArgumentDefinition> getArgumentSchema();

    AccessLevel getAccessLevel();

    default SequencedMap<String, ArgumentDefinition> getFullArgumentSchema() {
        SequencedMap<String, ArgumentDefinition> schema = new LinkedHashMap<>();

        schema.put("status", new ArgumentDefinition(ArgumentType.STRING, "Status shown to the player while this tool runs, in the language of your replies: 2-5 words describing only this action, ending with '...', e.g. 'Checking inventory...'. No reasoning, explanations or results."));
        schema.putAll(getArgumentSchema());

        return schema;
    }
}
