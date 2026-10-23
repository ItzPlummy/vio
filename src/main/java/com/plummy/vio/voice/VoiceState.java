package com.plummy.vio.voice;

public enum VoiceState {

    IDLE("idle"),
    LISTENING("listening"),
    // Listening, but no audio has arrived yet: muted, or push-to-talk isn't held.
    NO_AUDIO("no_audio"),
    // Capture ended; the last phrase is still being transcribed.
    TRANSCRIBING("transcribing"),
    SPEAKING("speaking");

    private final String value;

    VoiceState(String value) {
        this.value = value;
    }

    public static VoiceState fromValue(String value) {
        for (VoiceState state : values()) {
            if (state.getValue().equals(value)) {
                return state;
            }
        }

        throw new IllegalArgumentException("Invalid voice state: " + value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
