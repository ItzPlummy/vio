package com.plummy.vio.tool;

public enum ToolExecutionStatus {

    SUCCESS("success"),
    PARTIAL("partial"),
    INVALID_ARGUMENTS("invalid_arguments"),
    DENIED("denied"),
    FAILURE("failure");

    private final String value;

    ToolExecutionStatus(String value) {
        this.value = value;
    }

    public static ToolExecutionStatus fromValue(String value) {
        for (ToolExecutionStatus status : values()) {
            if (status.getValue().equals(value)) {
                return status;
            }
        }

        throw new IllegalArgumentException("Invalid tool execution status: " + value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
