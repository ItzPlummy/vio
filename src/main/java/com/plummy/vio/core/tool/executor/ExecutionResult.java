package com.plummy.vio.core.tool.executor;

import com.google.gson.JsonObject;

public class ExecutionResult {

    private final ExecutionStatus status;
    private final String result;

    public ExecutionResult(ExecutionStatus status, String result) {
        this.status = status;
        this.result = result;
    }

    public static ExecutionResult success(String result) {
        return new ExecutionResult(ExecutionStatus.SUCCESS, result);
    }

    public static ExecutionResult success(JsonObject result) {
        return success(result.toString());
    }

    public static ExecutionResult invalidArguments(String result) {
        return new ExecutionResult(ExecutionStatus.INVALID_ARGUMENTS, result);
    }

    public static ExecutionResult denied(String result) {
        return new ExecutionResult(ExecutionStatus.DENIED, result);
    }

    public static ExecutionResult failure(String result) {
        return new ExecutionResult(ExecutionStatus.FAILURE, result);
    }

    public ExecutionStatus getStatus() {
        return status;
    }

    public String getResult() {
        return result;
    }
}
