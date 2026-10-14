package com.plummy.vio.conversation;

import com.plummy.vio.message.ConversationMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerConversation implements Conversation {

    private final UUID playerId;
    private final List<ConversationMessage> messages = new ArrayList<>();

    public PlayerConversation(UUID playerId) {
        this.playerId = playerId;
    }

    @Override
    public UUID getId() {
        return playerId;
    }

    @Override
    public ConversationType getType() {
        return ConversationType.PLAYER;
    }

    @Override
    public List<ConversationMessage> getMessages() {
        return messages;
    }

    @Override
    public void addMessage(ConversationMessage message) {
        messages.add(message);
    }

    @Override
    public <T> T accept(ConversationVisitor<T> visitor) {
        return visitor.visitPlayerConversation(this);
    }
}
