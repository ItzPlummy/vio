package com.plummy.vio.core.tool;

import com.plummy.vio.core.access.AccessLevel;
import com.plummy.vio.core.tool.argument.ArgumentDefinition;

import java.util.SequencedMap;

public sealed interface Tool permits BasicTool, ServerTool {

    String getName();

    String getDescription();

    SequencedMap<String, ArgumentDefinition> getArgumentSchema();

    AccessLevel getAccessLevel();

    default boolean isEnabled() {
        return true;
    }
}
