package com.plummy.vio.registry;

import com.plummy.vio.tool.SetTimeTool;
import com.plummy.vio.tool.Tool;
import com.plummy.vio.tool.WebSearchTool;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

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

    public static Collection<Tool> getAll() {
        return Collections.unmodifiableCollection(TOOLS.values());
    }
}
