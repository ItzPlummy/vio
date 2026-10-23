package com.plummy.vio.tool;

public enum ExecutionStatus {

    SUCCESS("success"),
    PARTIAL("partial"),
    INVALID_ARGUMENTS("invalid_arguments"),
    DENIED("denied"),
    FAILURE("failure");

    private final String value;

    ExecutionStatus(String value) {
        this.value = value;
    }

    public static ExecutionStatus fromValue(String value) {
        for (ExecutionStatus status : values()) {
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
