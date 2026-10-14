package com.plummy.vio.conversation;

public enum ConversationType {

    PLAYER("player");

    private final String value;

    ConversationType(String value) {
        this.value = value;
    }

    public static ConversationType fromValue(String value) {
        for (ConversationType type : values()) {
            if (type.getValue().equals(value)) {
                return type;
            }
        }

        throw new IllegalArgumentException("Invalid conversation type: " + value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
