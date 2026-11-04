package com.plummy.vio.core.script;

public enum ScriptType {

    ONE_SHOT("one_shot"),
    PERIODIC("periodic"),
    COMMAND("command"),
    LISTENER("listener");

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
}
