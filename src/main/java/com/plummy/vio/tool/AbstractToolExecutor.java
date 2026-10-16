package com.plummy.vio.tool;

import com.plummy.vio.access.AccessLevel;
import com.plummy.vio.registry.ToolRegistry;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractToolExecutor implements ToolExecutor {

    @Override
    public List<Tool> getAvailableTools(AccessLevel accessLevel) {
        List<Tool> available = new ArrayList<>();

        for (Tool tool : ToolRegistry.getAll()) {
            if (accessLevel.isAtLeast(tool.getAccessLevel())) {
                available.add(tool);
            }
        }

        return available;
    }
}
