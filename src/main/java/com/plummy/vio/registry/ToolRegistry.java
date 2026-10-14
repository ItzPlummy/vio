package com.plummy.vio.registry;

import com.plummy.vio.tool.SetTimeTool;
import com.plummy.vio.tool.Tool;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class ToolRegistry {

    private static final Map<String, Tool> TOOLS = new LinkedHashMap<>();

    public static final Tool SET_TIME = register(new SetTimeTool());

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

    public static Set<String> getNames() {
        return Collections.unmodifiableSet(TOOLS.keySet());
    }
}
