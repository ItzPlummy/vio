package com.plummy.vio.client.message;

public enum ClientMessageType {

    PLAYER("player"),
    AGENT("agent"),
    STATUS("status"),
    ERROR("error");

    private final String value;

    ClientMessageType(String value) {
        this.value = value;
    }

    public static ClientMessageType fromValue(String value) {
        for (ClientMessageType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }

        throw new IllegalArgumentException("Invalid client message type: %s".formatted(value));
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
