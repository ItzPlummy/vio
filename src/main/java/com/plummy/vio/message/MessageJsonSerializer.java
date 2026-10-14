package com.plummy.vio.message;

import com.google.gson.JsonObject;
import com.plummy.vio.registries.VisitorRegistry;

public class MessageJsonSerializer implements MessageVisitor<JsonObject> {

    @Override
    public JsonObject visitSystemMessage(SystemMessage message) {
        JsonObject json = new JsonObject();

        json.addProperty("type", message.getType().getValue());
        json.add("text", message.getText().accept(VisitorRegistry.SECTION_JSON_SERIALIZER));

        return json;
    }

    @Override
    public JsonObject visitPlayerRequestMessage(PlayerRequestMessage message) {
        JsonObject json = new JsonObject();

        json.addProperty("type", message.getType().getValue());
        json.add("request", message.getRequest().accept(VisitorRegistry.SECTION_JSON_SERIALIZER));

        return json;
    }

    @Override
    public JsonObject visitAgentResponseMessage(AgentResponseMessage message) {
        JsonObject json = new JsonObject();

        json.addProperty("type", message.getType().getValue());
        json.add("response", message.getResponse().accept(VisitorRegistry.SECTION_JSON_SERIALIZER));

        return json;
    }
}
