package com.plummy.vio.core.tool.executor;

public record ExecutionResult(ExecutionStatus status, String message) {

    public static ExecutionResult success(String message) {
        return new ExecutionResult(ExecutionStatus.SUCCESS, message);
    }

    public static ExecutionResult unprocessable(String message) {
        return new ExecutionResult(ExecutionStatus.UNPROCESSABLE, message);
    }

    public static ExecutionResult denied(String message) {
        return new ExecutionResult(ExecutionStatus.DENIED, message);
    }

    public static ExecutionResult failure(String message) {
        return new ExecutionResult(ExecutionStatus.FAILURE, message);
    }
}
