package com.plummy.vio.conversation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.plummy.vio.message.Message;
import com.plummy.vio.message.MessageJsonSerializer;

public class ConversationJsonSerializer implements ConversationVisitor<JsonObject> {

    private static final ConversationJsonSerializer INSTANCE = new ConversationJsonSerializer();

    public static ConversationJsonSerializer getInstance() {
        return INSTANCE;
    }

    private ConversationJsonSerializer() {
    }

    @Override
    public JsonObject visitPlayerConversation(PlayerConversation conversation) {
        JsonObject json = new JsonObject();

        JsonArray messages = new JsonArray();
        for (Message message : conversation.getMessages()) {
            messages.add(message.accept(MessageJsonSerializer.getInstance()));
        }

        json.add("messages", messages);

        return json;
    }
}
