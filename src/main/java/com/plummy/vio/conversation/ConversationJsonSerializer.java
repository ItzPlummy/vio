package com.plummy.vio.conversation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.plummy.vio.message.ConversationMessage;
import com.plummy.vio.registry.VisitorRegistry;

public class ConversationJsonSerializer implements ConversationVisitor<JsonObject> {

    @Override
    public JsonObject visitPlayerConversation(PlayerConversation conversation) {
        JsonObject json = new JsonObject();

        JsonArray messages = new JsonArray();
        for (ConversationMessage message : conversation.getMessages()) {
            messages.add(message.accept(VisitorRegistry.MESSAGE_JSON_SERIALIZER));
        }

        json.addProperty("player_id", conversation.getId().toString());
        json.add("system_message", conversation.getSystemMessage().accept(VisitorRegistry.MESSAGE_JSON_SERIALIZER));
        json.add("messages", messages);

        return json;
    }
}
