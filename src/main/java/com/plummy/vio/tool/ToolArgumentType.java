package com.plummy.vio.tool;

public enum ToolArgumentType {

    INTEGER("integer"),
    FLOAT("number"),
    STRING("string"),
    BOOLEAN("boolean");

    private final String value;

    ToolArgumentType(String value) {
        this.value = value;
    }

    public static ToolArgumentType fromValue(String value) {
        for (ToolArgumentType type : values()) {
            if (type.getValue().equals(value)) {
                return type;
            }
        }

        throw new IllegalArgumentException("Invalid tool argument type: " + value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
