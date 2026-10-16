package com.plummy.vio.agent;

public enum SubmitResult {

    ACCEPTED("accepted"),
    BUSY("busy"),
    DENIED("denied"),
    UNAVAILABLE("unavailable");

    private final String value;

    SubmitResult(String value) {
        this.value = value;
    }

    public static SubmitResult fromValue(String value) {
        for (SubmitResult result : values()) {
            if (result.getValue().equals(value)) {
                return result;
            }
        }

        throw new IllegalArgumentException("Invalid submit result: " + value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
