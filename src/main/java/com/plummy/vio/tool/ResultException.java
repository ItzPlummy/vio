package com.plummy.vio.tool;

// Lets shared helpers end a tool call with a finished result (denied, failure, ...) from any depth.
public class ResultException extends RuntimeException {

    private final ExecutionResult result;

    public ResultException(ExecutionResult result) {
        super(result.getResult());
        this.result = result;
    }

    public static ResultException denied(String message) {
        return new ResultException(ExecutionResult.denied(message));
    }

    public static ResultException failure(String message) {
        return new ResultException(ExecutionResult.failure(message));
    }

    public static ResultException invalidArguments(String message) {
        return new ResultException(ExecutionResult.invalidArguments(message));
    }

    public ExecutionResult getResult() {
        return result;
    }
}
