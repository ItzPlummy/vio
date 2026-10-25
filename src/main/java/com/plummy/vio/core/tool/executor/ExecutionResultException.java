package com.plummy.vio.core.tool.executor;

public class ExecutionResultException extends RuntimeException {

    private final ExecutionResult result;

    public ExecutionResultException(ExecutionResult result) {
        super(result.getResult());
        this.result = result;
    }

    public static ExecutionResultException invalidArguments(String message) {
        return new ExecutionResultException(ExecutionResult.invalidArguments(message));
    }

    public static ExecutionResultException denied(String message) {
        return new ExecutionResultException(ExecutionResult.denied(message));
    }

    public static ExecutionResultException failure(String message) {
        return new ExecutionResultException(ExecutionResult.failure(message));
    }

    public ExecutionResult getResult() {
        return result;
    }
}
