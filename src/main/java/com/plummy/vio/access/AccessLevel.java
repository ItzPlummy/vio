package com.plummy.vio.access;

public enum AccessLevel {

    NONE("none"),
    BASIC("basic"),
    FULL("full");

    private final String value;

    AccessLevel(String value) {
        this.value = value;
    }

    public static AccessLevel fromValue(String value) {
        for (AccessLevel level : values()) {
            if (level.getValue().equals(value)) {
                return level;
            }
        }

        throw new IllegalArgumentException("Invalid access level: %s".formatted(value));
    }

    public String getValue() {
        return value;
    }

    public boolean isAtLeast(AccessLevel other) {
        return compareTo(other) >= 0;
    }

    @Override
    public String toString() {
        return value;
    }
}
