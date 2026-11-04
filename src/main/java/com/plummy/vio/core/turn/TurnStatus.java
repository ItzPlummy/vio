package com.plummy.vio.core.turn;

public enum TurnStatus {

    PENDING("pending"),
    COMPLETED("completed");

    private final String value;

    TurnStatus(String value) {
        this.value = value;
    }

    public static TurnStatus fromValue(String value) {
        for (TurnStatus status : TurnStatus.values()) {
            if (status.getValue().equals(value)) {
                return status;
            }
        }

        throw new IllegalArgumentException("Invalid turn status: " + value);
    }

    public String getValue() {
        return value;
    }
}
