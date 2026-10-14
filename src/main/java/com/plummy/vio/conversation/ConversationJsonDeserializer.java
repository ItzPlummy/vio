package com.plummy.vio.conversation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.plummy.vio.message.ConversationMessage;
import com.plummy.vio.message.MessageJsonDeserializer;

import java.util.UUID;

public class ConversationJsonDeserializer {

    private static final ConversationJsonDeserializer INSTANCE = new ConversationJsonDeserializer();

    public static ConversationJsonDeserializer getInstance() {
        return INSTANCE;
    }

    private ConversationJsonDeserializer() {
    }

    public Conversation deserialize(JsonObject json) {
        Conversation conversation = switch (ConversationType.fromValue(json.get("type").getAsString())) {
            case PLAYER -> new PlayerConversation(
                    UUID.fromString(json.get("player_id").getAsString())
            );
        };

        for (JsonElement message : json.getAsJsonArray("messages")) {
            conversation.addMessage(MessageJsonDeserializer.getInstance().deserialize(message.getAsJsonObject(), ConversationMessage.class));
        }

        return conversation;
    }
}
