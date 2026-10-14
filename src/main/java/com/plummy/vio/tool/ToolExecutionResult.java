package com.plummy.vio.tool;

public class ToolExecutionResult {

    private final ToolExecutionStatus status;
    private final String result;

    public ToolExecutionResult(ToolExecutionStatus status, String result) {
        this.status = status;
        this.result = result;
    }

    public static ToolExecutionResult success(String result) {
        return new ToolExecutionResult(ToolExecutionStatus.SUCCESS, result);
    }

    public static ToolExecutionResult partial(String result) {
        return new ToolExecutionResult(ToolExecutionStatus.PARTIAL, result);
    }

    public static ToolExecutionResult invalidArguments(String result) {
        return new ToolExecutionResult(ToolExecutionStatus.INVALID_ARGUMENTS, result);
    }

    public static ToolExecutionResult denied(String result) {
        return new ToolExecutionResult(ToolExecutionStatus.DENIED, result);
    }

    public static ToolExecutionResult failure(String result) {
        return new ToolExecutionResult(ToolExecutionStatus.FAILURE, result);
    }

    public ToolExecutionStatus getStatus() {
        return status;
    }

    public String getResult() {
        return result;
    }
}
