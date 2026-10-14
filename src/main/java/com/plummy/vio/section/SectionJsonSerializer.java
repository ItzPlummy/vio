package com.plummy.vio.section;

import com.google.gson.JsonObject;

public class SectionJsonSerializer implements SectionVisitor<JsonObject> {

    @Override
    public JsonObject visitTextSection(TextSection section) {
        JsonObject json = new JsonObject();

        json.addProperty("type", section.getType().getValue());
        json.addProperty("content", section.getText());

        return json;
    }
}
