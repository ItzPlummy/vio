package com.plummy.vio.section;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class SectionJsonSerializer implements SectionVisitor<JsonObject> {

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
        JsonObject json = new JsonObject();

        json.addProperty("type", section.getType().getValue());
        json.addProperty("id", section.getId().toString());
        json.addProperty("name", section.getName());
        json.addProperty("x", section.getX());
        json.addProperty("y", section.getY());
        json.addProperty("z", section.getZ());
        json.addProperty("yaw", section.getYaw());
        json.addProperty("pitch", section.getPitch());

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

        return json;
    }
}
