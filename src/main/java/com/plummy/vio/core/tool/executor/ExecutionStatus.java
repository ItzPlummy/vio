package com.plummy.vio.core.tool.executor;

public enum ExecutionStatus {

    SUCCESS("success"),
    UNPROCESSABLE("unprocessable"),
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

        throw new IllegalArgumentException("Invalid execution status: " + value);
    }

    public String getValue() {
        return value;
    }
}
