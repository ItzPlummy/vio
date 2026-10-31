package com.plummy.vio.conversation;

import com.google.gson.JsonObject;
import com.plummy.vio.storage.JsonDirectoryStorage;

public abstract class ConversationStorage<K, V extends Conversation<K>> extends JsonDirectoryStorage<K, V> {

    @Override
    protected JsonObject serializeItemToJson(V item) {
        return item.accept(ConversationJsonSerializer.getInstance());
    }
}
