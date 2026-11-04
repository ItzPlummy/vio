package com.plummy.vio.core.tool;

import com.plummy.vio.tool.Tool;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ToolRegistry {

    private static final Map<String, Tool> TOOLS = new LinkedHashMap<>();

    private static <T extends Tool> T register(T tool) {
        if (TOOLS.putIfAbsent(tool.getName(), tool) != null) {
            throw new IllegalStateException("Duplicate tool name: %s".formatted(tool.getName()));
        }

        return tool;
    }

    public static Tool get(String name) {
        return TOOLS.get(name);
    }

    public static Collection<Tool> getAll() {
        return List.copyOf(TOOLS.values());
    }
}
