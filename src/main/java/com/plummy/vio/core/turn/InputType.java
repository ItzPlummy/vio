package com.plummy.vio.core.turn;

public enum InputType {

    TEXT("text"),
    VOICE("voice");

    private final String value;

    InputType(String value) {
        this.value = value;
    }

    public static InputType fromValue(String value) {
        for (InputType type : values()) {
            if (type.getValue().equals(value)) {
                return type;
            }
        }

        throw new IllegalArgumentException("Invalid input type: " + value);
    }

    public String getValue() {
        return value;
    }
}
