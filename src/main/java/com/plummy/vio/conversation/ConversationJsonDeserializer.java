package com.plummy.vio.conversation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.plummy.vio.message.ConversationMessage;
import com.plummy.vio.message.SystemMessage;
import com.plummy.vio.registry.DeserializerRegistry;

import java.util.UUID;

public class ConversationJsonDeserializer {

    public Conversation deserialize(JsonObject json) {
        Conversation conversation = switch (ConversationType.fromValue(json.get("type").getAsString())) {
            case PLAYER -> new PlayerConversation(
                    UUID.fromString(json.get("player_id").getAsString()),
                    DeserializerRegistry.MESSAGE_JSON_DESERIALIZER.deserialize(json.getAsJsonObject("system_message"), SystemMessage.class)
            );
        };

        for (JsonElement message : json.getAsJsonArray("messages")) {
            conversation.addMessage(
                    DeserializerRegistry.MESSAGE_JSON_DESERIALIZER.deserialize(message.getAsJsonObject(), ConversationMessage.class)
            );
        }

        return conversation;
    }
}
