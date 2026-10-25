package com.plummy.vio.core.tool;

public enum ToolType {

    BASIC("basic"),
    SERVER("server");

    private final String value;

    ToolType(String value) {
        this.value = value;
    }

    public static ToolType fromValue(String value) {
        for (ToolType type : ToolType.values()) {
            if (type.getValue().equals(value)) {
                return type;
            }
        }

        throw new IllegalArgumentException("Invalid tool type: " + value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
