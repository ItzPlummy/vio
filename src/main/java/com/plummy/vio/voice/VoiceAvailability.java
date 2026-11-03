package com.plummy.vio.voice;

public enum VoiceAvailability {

    AVAILABLE("available"),
    // The server has no Simple Voice Chat.
    UNSUPPORTED("unsupported"),
    // The player's client has no Simple Voice Chat.
    NOT_INSTALLED("not_installed"),
    DISCONNECTED("disconnected"),
    // The player turned voice chat off.
    DISABLED("disabled");

    private final String value;

    VoiceAvailability(String value) {
        this.value = value;
    }

    public static VoiceAvailability fromValue(String value) {
        for (VoiceAvailability availability : values()) {
            if (availability.getValue().equals(value)) {
                return availability;
            }
        }

        throw new IllegalArgumentException("Invalid voice availability: %s".formatted(value));
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
