package com.plummy.vio.tool;

import com.plummy.vio.access.AccessLevel;

import java.util.List;

public interface ToolExecutor<T extends Tool> {

    List<T> getAvailableTools(AccessLevel accessLevel);

    Tool.ExecutionResult execute(AccessLevel accessLevel, Tool.ExecutionRequest request);
}
