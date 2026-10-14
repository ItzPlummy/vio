package com.plummy.vio.conversation;

import com.plummy.vio.message.ConversationMessage;
import com.plummy.vio.storage.Storage;

import java.util.List;
import java.util.UUID;

public interface Conversation extends Storage.Item<UUID> {

    UUID getId();

    ConversationType getType();

    List<ConversationMessage> getMessages();

    void addMessage(ConversationMessage message);

    <T> T accept(ConversationVisitor<T> visitor);

    default UUID getKey() {
        return getId();
    }
}
