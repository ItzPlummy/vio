package com.plummy.vio.script;

public enum ScriptState {

    ENABLED("enabled"),
    DISABLED("disabled"),
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

        throw new IllegalArgumentException("Invalid script state: %s".formatted(value));
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
