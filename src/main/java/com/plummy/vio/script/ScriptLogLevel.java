package com.plummy.vio.script;

public enum ScriptLogLevel {

    INFO("info"),
    WARNING("warning"),
    ERROR("error");

    private final String value;

    ScriptLogLevel(String value) {
        this.value = value;
    }

    public static ScriptLogLevel fromValue(String value) {
        for (ScriptLogLevel scriptLogLevel : values()) {
            if (scriptLogLevel.value.equals(value)) {
                return scriptLogLevel;
            }
        }

        throw new IllegalArgumentException("Invalid script log level: " + value);
    }

    public String getValue() {
        return value;
    }

    public boolean isAtLeast(ScriptLogLevel other) {
        return compareTo(other) >= 0;
    }

    @Override
    public String toString() {
        return getValue();
    }

}
