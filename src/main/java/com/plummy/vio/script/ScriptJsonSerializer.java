package com.plummy.vio.script;

import com.google.gson.JsonObject;

public class ScriptJsonSerializer implements ScriptVisitor<JsonObject> {

    private static final ScriptJsonSerializer INSTANCE = new ScriptJsonSerializer();

    public static ScriptJsonSerializer getInstance() {
        return INSTANCE;
    }

    private ScriptJsonSerializer() {
    }

    @Override
    public JsonObject visitOneShotScript(OneShotScript script) {
        JsonObject json = new JsonObject();

        json.addProperty("type", script.getType().getValue());
        json.addProperty("description", script.getDescription());

        return json;
    }

    @Override
    public JsonObject visitPeriodicScript(PeriodicScript script) {
        JsonObject json = new JsonObject();

        json.addProperty("type", script.getType().getValue());
        json.addProperty("description", script.getDescription());
        json.addProperty("is_enabled", script.isEnabled());
        json.addProperty("period_ticks", script.getPeriodTicks());

        return json;
    }

    @Override
    public JsonObject visitCommandScript(CommandScript script) {
        JsonObject json = new JsonObject();

        json.addProperty("type", script.getType().getValue());
        json.addProperty("description", script.getDescription());
        json.addProperty("is_enabled", script.isEnabled());

        return json;
    }

    @Override
    public JsonObject visitListenerScript(ListenerScript script) {
        JsonObject json = new JsonObject();

        json.addProperty("type", script.getType().getValue());
        json.addProperty("description", script.getDescription());
        json.addProperty("is_enabled", script.isEnabled());

        return json;
    }
}
