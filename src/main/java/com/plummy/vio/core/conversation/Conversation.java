package com.plummy.vio.core.conversation;

import com.plummy.vio.core.message.Message;
import com.plummy.vio.core.storage.Storage;

import java.util.List;
import java.util.UUID;

public interface Conversation extends Storage.Item<UUID> {

    UUID getId();

    ConversationType getType();

    List<Message> getMessages();

    void addMessage(Message message);

    <T> T accept(ConversationVisitor<T> visitor);

    default UUID getKey() {
        return getId();
    }
}
