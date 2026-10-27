package com.plummy.vio.conversation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.plummy.vio.message.MessageJsonDeserializer;

import java.util.UUID;

public class ConversationJsonDeserializer {

    private static final ConversationJsonDeserializer INSTANCE = new ConversationJsonDeserializer();

    public static ConversationJsonDeserializer getInstance() {
        return INSTANCE;
    }

    private ConversationJsonDeserializer() {
    }

    public PlayerConversation deserializePlayerConversation(UUID playerId, JsonObject json) {
        PlayerConversation conversation = new PlayerConversation(playerId);

        for (JsonElement message : json.getAsJsonArray("messages")) {
            conversation.addMessage(MessageJsonDeserializer.getInstance().deserialize(message.getAsJsonObject()));
        }

        return conversation;
    }
}
