package com.plummy.vio.script;

import java.util.Objects;
import java.util.regex.Pattern;

public class ScriptKey {

    public static final String RULE = "1 to 64 lowercase letters, digits, underscores and dashes";

    private static final Pattern VALID_KEY = Pattern.compile("[a-z0-9_-]{1,64}");

    private final String key;

    public ScriptKey(String key) {
        if (key == null || !VALID_KEY.matcher(key).matches()) {
            throw new ScriptException("Invalid script name '%s': names are %s".formatted(key, RULE));
        }

        this.key = key;
    }

    public String getKey() {
        return key;
    }

    @Override
    public String toString() {
        return getKey();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof ScriptKey scriptKey)) {
            return false;
        }

        return getKey().equals(scriptKey.getKey());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getKey());
    }
}
