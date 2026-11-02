package com.plummy.vio.script.state;

import com.google.gson.JsonObject;
import com.plummy.vio.script.ScriptException;
import com.plummy.vio.storage.StorageItem;

import java.util.regex.Pattern;

public final class StateItem implements StorageItem<String> {

    public static final String KEY_RULE = "1 to 64 lowercase letters, digits, underscores, dashes and dots";

    private static final Pattern VALID_KEY = Pattern.compile("[a-z0-9_.-]{1,64}");

    private final String key;
    private final JsonObject data;

    public StateItem(String key, JsonObject data) {
        if (key == null || !VALID_KEY.matcher(key).matches()) {
            throw new ScriptException("Invalid state key '%s': keys are %s".formatted(key, KEY_RULE));
        }

        if (data == null) {
            throw new ScriptException("State item '%s' needs data, got null".formatted(key));
        }

        this.key = key;
        this.data = data.deepCopy();
    }

    @Override
    public String getKey() {
        return key;
    }

    // A copy, so changing it never changes the stored item
    public JsonObject getData() {
        return data.deepCopy();
    }
}
