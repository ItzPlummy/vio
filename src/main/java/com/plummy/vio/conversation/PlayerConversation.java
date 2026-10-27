package com.plummy.vio.conversation;

import com.plummy.vio.config.AgentLimits;
import com.plummy.vio.message.Message;
import com.plummy.vio.message.PlayerRequestMessage;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

public class PlayerConversation implements Conversation {

    private final UUID playerId;
    private final Deque<Message> messages = new ArrayDeque<>();

    public PlayerConversation(UUID playerId) {
        this.playerId = playerId;
    }

    @Override
    public String getKey() {
        return getPlayerId().toString();
    }

    @Override
    public ConversationType getType() {
        return ConversationType.PLAYER;
    }

    @Override
    public List<Message> getMessages() {
        return List.copyOf(messages);
    }

    @Override
    public void addMessage(Message message) {
        messages.add(message);

        while (!messages.isEmpty() && (messages.size() > AgentLimits.MAX_CONVERSATION_MESSAGES || !(messages.getFirst() instanceof PlayerRequestMessage))) {
            messages.removeFirst();
        }
    }

    @Override
    public <T> T accept(ConversationVisitor<T> visitor) {
        return visitor.visitPlayerConversation(this);
    }

    public UUID getPlayerId() {
        return playerId;
    }
}
