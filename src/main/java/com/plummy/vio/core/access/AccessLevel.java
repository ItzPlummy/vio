package com.plummy.vio.core.access;

public enum AccessLevel {

    NONE("none"),
    BASIC("basic"),
    FULL("full");

    private final String value;

    AccessLevel(String value) {
        this.value = value;
    }

    public static AccessLevel fromValue(String value) {
        for (AccessLevel accessLevel : values()) {
            if (accessLevel.getValue().equals(value)) {
                return accessLevel;
            }
        }

        throw new IllegalArgumentException("Invalid access level: " + value);
    }

    public String getValue() {
        return value;
    }

    public boolean isAtLeast(AccessLevel other) {
        return this.ordinal() >= other.ordinal();
    }
}
