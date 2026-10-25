package com.plummy.vio.core.message;

public enum MessageType {

    PLAYER_REQUEST("player_request"),
    TOOL_REQUEST("tool_request"),
    TOOL_RESPONSE("tool_response"),
    AGENT_RESPONSE("agent_response");

    private final String value;

    MessageType(String value) {
        this.value = value;
    }

    public static MessageType fromValue(String value) {
        for (MessageType messageType : MessageType.values()) {
            if (messageType.getValue().equals(value)) {
                return messageType;
            }
        }

        throw new IllegalArgumentException("Invalid message type: " + value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
