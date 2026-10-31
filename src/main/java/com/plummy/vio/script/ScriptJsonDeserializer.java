package com.plummy.vio.script;

import com.google.gson.JsonObject;

public class ScriptJsonDeserializer {

    private static final ScriptJsonDeserializer INSTANCE = new ScriptJsonDeserializer();

    public static ScriptJsonDeserializer getInstance() {
        return INSTANCE;
    }

    private ScriptJsonDeserializer() {
    }

    public Script deserialize(ScriptKey key, JsonObject json) {
        return switch (ScriptType.fromValue(json.get("type").getAsString())) {
            case ONE_SHOT -> new OneShotScript(
                    key,
                    json.get("description").getAsString()
            );
            case PERIODIC -> new PeriodicScript(
                    key,
                    json.get("description").getAsString(),
                    json.get("is_enabled").getAsBoolean(),
                    json.get("period_ticks").getAsLong()
            );
            case COMMAND -> new CommandScript(
                    key,
                    json.get("description").getAsString(),
                    json.get("is_enabled").getAsBoolean()
            );
            case LISTENER -> new ListenerScript(
                    key,
                    json.get("description").getAsString(),
                    json.get("is_enabled").getAsBoolean()
            );
        };
    }
}
