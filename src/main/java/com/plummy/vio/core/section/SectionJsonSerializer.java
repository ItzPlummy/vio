package com.plummy.vio.core.section;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class SectionJsonSerializer implements SectionVisitor<JsonObject> {

    private static final SectionJsonSerializer INSTANCE = new SectionJsonSerializer();

    public static SectionJsonSerializer getInstance() {
        return INSTANCE;
    }

    private SectionJsonSerializer() {
    }

    @Override
    public JsonObject visitTextSection(TextSection section) {
        JsonObject json = new JsonObject();

        json.addProperty("type", section.getType().getValue());
        json.addProperty("text", section.getText());

        return json;
    }

    @Override
    public JsonObject visitActivityDescriptionSection(ActivityDescriptionSection section) {
        JsonObject json = new JsonObject();

        json.addProperty("type", section.getType().getValue());
        json.addProperty("activity_description", section.getActivityDescription());

        return json;
    }

    @Override
    public JsonObject visitPlayerRequestSection(PlayerRequestSection section) {
        JsonObject json = new JsonObject();

        json.addProperty("type", section.getType().getValue());
        json.addProperty("request", section.getRequest());

        return json;
    }

    @Override
    public JsonObject visitPlayerContextSection(PlayerContextSection section) {
        JsonArray structures = new JsonArray();

        for (String structure : section.getStructures()) {
            structures.add(structure);
        }

        JsonObject json = new JsonObject();

        json.addProperty("type", section.getType().getValue());
        json.addProperty("id", section.getId().toString());
        json.addProperty("name", section.getName());
        json.addProperty("access_level", section.getAccessLevel().getValue());
        json.addProperty("x", section.getX());
        json.addProperty("y", section.getY());
        json.addProperty("z", section.getZ());
        json.addProperty("yaw", section.getYaw());
        json.addProperty("pitch", section.getPitch());
        json.addProperty("dimension", section.getDimension());
        json.addProperty("biome", section.getBiome());
        json.add("structures", structures);
        json.addProperty("health", section.getHealth());
        json.addProperty("armor", section.getArmor());
        json.addProperty("food", section.getFood());
        json.addProperty("saturation", section.getSaturation());
        json.addProperty("level", section.getLevel());
        json.addProperty("game_mode", section.getGameMode());
        json.addProperty("main_hand_item", section.getMainHandItem());
        json.addProperty("off_hand_item", section.getOffHandItem());

        return json;
    }

    @Override
    public JsonObject visitServerContextSection(ServerContextSection section) {
        JsonArray dimensions = new JsonArray();

        for (String dimension : section.getDimensions()) {
            dimensions.add(dimension);
        }

        JsonObject json = new JsonObject();

        json.addProperty("type", section.getType().getValue());
        json.add("dimensions", dimensions);
        json.addProperty("time", section.getTime());
        json.addProperty("day", section.getDay());

        return json;
    }

    @Override
    public JsonObject visitScriptContextSection(ScriptContextSection section) {
        JsonArray scripts = new JsonArray();

        for (ScriptContextSection.Entry script : section.getScripts()) {
            JsonObject scriptJson = new JsonObject();

            scriptJson.addProperty("name", script.name());
            scriptJson.addProperty("type", script.type().getValue());
            scriptJson.addProperty("state", script.state().getValue());
            scriptJson.addProperty("description", script.description());

            scripts.add(scriptJson);
        }

        JsonObject json = new JsonObject();

        json.addProperty("type", section.getType().getValue());
        json.add("scripts", scripts);

        return json;
    }
}
