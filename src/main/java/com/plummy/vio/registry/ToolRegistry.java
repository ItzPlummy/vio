package com.plummy.vio.registry;

import com.plummy.vio.tool.SetTimeTool;
import com.plummy.vio.tool.Tool;
import com.plummy.vio.tool.WebSearchTool;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ToolRegistry {

    private static final Map<String, Tool> TOOLS = new LinkedHashMap<>();

    public static Tool SET_TIME = register(new SetTimeTool());
    public static Tool WEB_SEARCH = register(new WebSearchTool());

    private static Tool register(Tool tool) {
        if (TOOLS.putIfAbsent(tool.getName(), tool) != null) {
            throw new IllegalStateException("Duplicate tool name: " + tool.getName());
        }

        return tool;
    }

    public static Tool get(String name) {
        return TOOLS.get(name);
    }

    public static <T> T get(String name, Class<T> toolClass) {
        Tool tool = TOOLS.get(name);

        if (!toolClass.isInstance(tool)) {
            return null;
        }

        return toolClass.cast(tool);
    }

    public static Collection<Tool> getAll() {
        return Collections.unmodifiableCollection(TOOLS.values());
    }

    public static <T> Collection<T> getAll(Class<T> toolClass) {
        return TOOLS.values().stream().filter(toolClass::isInstance).map(toolClass::cast).collect(Collectors.toList());
    }
}
