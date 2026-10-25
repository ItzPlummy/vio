package com.plummy.vio.core.tool;

import com.plummy.vio.core.access.AccessLevel;
import com.plummy.vio.core.tool.value.argument.ArgumentDefinition;

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
