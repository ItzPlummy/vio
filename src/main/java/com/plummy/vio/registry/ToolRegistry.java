package com.plummy.vio.registry;

import com.plummy.vio.tool.*;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ToolRegistry {

    private static final Map<String, Tool> TOOLS = new LinkedHashMap<>();

    public static ServerTool SET_TIME = register(new SetTimeTool());

    public static BasicTool WEB_SEARCH = register(new WebSearchTool());
    public static BasicTool WEB_FETCH = register(new WebFetchTool());

    private static <T extends Tool> T register(T tool) {
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
