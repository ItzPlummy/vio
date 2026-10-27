package com.plummy.vio.conversation;

import com.google.gson.JsonObject;
import com.plummy.vio.storage.JsonDirectoryStorage;

public abstract class ConversationStorage<K, C extends Conversation<K>> extends JsonDirectoryStorage<K, C> {

    @Override
    protected JsonObject serializeItemToJson(C item) {
        return item.accept(ConversationJsonSerializer.getInstance());
    }
}
