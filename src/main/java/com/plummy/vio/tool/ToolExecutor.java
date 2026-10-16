package com.plummy.vio.tool;

import com.plummy.vio.access.AccessLevel;

public interface ToolExecutor {

    Tool.ExecutionResult execute(AccessLevel accessLevel, Tool.ExecutionRequest request);
}
