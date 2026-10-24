package com.plummy.vio.script;

public enum ScriptState {

    ENABLED("enabled"),
    DISABLED("disabled"),
    // Failed to compile, or disabled after repeated failures. Never saved: it's worked out again on every load.
    ERRORED("errored");

    private final String value;

    ScriptState(String value) {
        this.value = value;
    }

    public static ScriptState fromValue(String value) {
        for (ScriptState state : values()) {
            if (state.getValue().equals(value)) {
                return state;
            }
        }

        throw new IllegalArgumentException("Invalid script state: " + value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
