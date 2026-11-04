package com.plummy.vio.core.tool.executor;

public class ExecutionException extends RuntimeException {

    private final ExecutionResult result;

    public ExecutionException(String message, ExecutionResult result) {
        super(message);
        this.result = result;
    }

    public ExecutionException(String message, Throwable cause, ExecutionResult result) {
        super(message, cause);
        this.result = result;
    }

    public ExecutionResult getResult() {
        return result;
    }
}
