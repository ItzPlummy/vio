package com.plummy.vio.script;

public enum ScriptType {

    // Saved and run on demand; the script body is the action.
    ONE_SHOT("one_shot"),
    // Runs every interval_ticks while enabled, and on demand.
    PERIODIC("periodic"),
    // Handles NeoForge events declared with on(...) while enabled.
    LISTENER("listener"),
    // Adds one server command, declared with command(...), while enabled.
    COMMAND("command");

    private final String value;

    ScriptType(String value) {
        this.value = value;
    }

    public static ScriptType fromValue(String value) {
        for (ScriptType type : values()) {
            if (type.getValue().equals(value)) {
                return type;
            }
        }

        throw new IllegalArgumentException("Invalid script type: " + value);
    }

    public String getValue() {
        return value;
    }

    public boolean isToggleable() {
        return this != ONE_SHOT;
    }

    @Override
    public String toString() {
        return value;
    }
}
