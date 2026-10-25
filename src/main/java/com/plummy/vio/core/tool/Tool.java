package com.plummy.vio.core.tool;

import com.plummy.vio.core.access.AccessLevel;
import com.plummy.vio.core.tool.argument.ArgumentDefinition;
import com.plummy.vio.core.tool.argument.ArgumentType;

import java.util.LinkedHashMap;
import java.util.SequencedMap;

public interface Tool {

    ToolType getType();

    String getName();

    String getDescription();

    SequencedMap<String, ArgumentDefinition> getArgumentSchema();

    AccessLevel getAccessLevel();

    // Unavailable tools aren't sent to the model at all, e.g. when their feature is turned off in the config.
    default boolean isAvailable() {
        return true;
    }

    default SequencedMap<String, ArgumentDefinition> getFullArgumentSchema() {
        SequencedMap<String, ArgumentDefinition> schema = new LinkedHashMap<>();

        schema.put("status", new ArgumentDefinition(ArgumentType.STRING, "Status shown to the player while this tool runs, in the language of your replies: 2-5 words describing only this action, ending with '...', e.g. 'Checking inventory...'. No reasoning, explanations or results."));
        schema.putAll(getArgumentSchema());

        return schema;
    }
}
