package com.plummy.vio.provider;

import com.google.gson.JsonObject;
import com.plummy.vio.message.AgentResponseMessage;
import com.plummy.vio.message.MessageVisitor;
import com.plummy.vio.message.PlayerRequestMessage;
import com.plummy.vio.message.SystemMessage;

public class OpenAIMessageJsonSerializer implements MessageVisitor<JsonObject> {

    @Override
    public JsonObject visitSystemMessage(SystemMessage message) {
        JsonObject json = new JsonObject();

        json.addProperty("type", "message");
        json.addProperty("role", "system");
        json.addProperty("content", message.getContent());

        return json;
    }

    @Override
    public JsonObject visitPlayerRequestMessage(PlayerRequestMessage message) {
        JsonObject json = new JsonObject();

        json.addProperty("type", "message");
        json.addProperty("role", "user");
        json.addProperty("content", message.getContent());

        return json;
    }

    @Override
    public JsonObject visitAgentResponseMessage(AgentResponseMessage message) {
        JsonObject json = new JsonObject();

        json.addProperty("type", "message");
        json.addProperty("role", "assistant");
        json.addProperty("content", message.getContent());

        return json;
    }
}
