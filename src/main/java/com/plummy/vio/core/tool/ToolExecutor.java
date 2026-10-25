package com.plummy.vio.core.tool;

public interface ToolExecutor {

    ExecutionResult execute(Requester requester, ExecutionRequest request);
}
