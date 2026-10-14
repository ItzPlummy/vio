package com.plummy.vio.conversation;

import com.plummy.vio.message.ConversationMessage;
import com.plummy.vio.message.SystemMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerConversation implements Conversation {

    private final UUID playerId;
    private final SystemMessage systemMessage;
    private final List<ConversationMessage> messages = new ArrayList<>();

    public PlayerConversation(UUID playerId, SystemMessage systemMessage) {
        this.playerId = playerId;
        this.systemMessage = systemMessage;
    }

    public static PlayerConversation of(UUID playerId, String systemMessage) {
        return new PlayerConversation(playerId, SystemMessage.of(systemMessage));
    }

    @Override
    public UUID getId() {
        return playerId;
    }

    @Override
    public SystemMessage getSystemMessage() {
        return systemMessage;
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
