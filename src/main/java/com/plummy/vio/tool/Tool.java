package com.plummy.vio.tool;

import com.plummy.vio.access.AccessLevel;
import com.plummy.vio.tool.value.argument.ArgumentDefinition;

import java.util.SequencedMap;

public interface Tool {

    String getName();

    String getDescription();

    SequencedMap<String, ArgumentDefinition> getArgumentSchema();

    AccessLevel getAccessLevel();

    default boolean isAvailable() {
        return true;
    }
}
