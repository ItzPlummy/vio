package com.plummy.vio.conversation;

import com.plummy.vio.core.Keyed;
import com.plummy.vio.message.ConversationMessage;

import java.util.List;
import java.util.UUID;

public interface Conversation extends Keyed<UUID> {

    UUID getId();

    ConversationType getType();

    List<ConversationMessage> getMessages();

    void addMessage(ConversationMessage message);

    <T> T accept(ConversationVisitor<T> visitor);

    default UUID getKey() {
        return getId();
    }

    default void addMessages(List<ConversationMessage> messages) {
        messages.forEach(this::addMessage);
    }
}
