package com.plummy.vio.script;

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
        for (ScriptType scriptType : values()) {
            if (scriptType.getValue().equals(value)) {
                return scriptType;
            }
        }

        throw new IllegalArgumentException("Invalid script type: " + value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return getValue();
    }
}
