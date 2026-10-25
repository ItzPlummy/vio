package com.plummy.vio.core.conversation;

import com.plummy.vio.core.message.Message;
import com.plummy.vio.core.message.PlayerRequestMessage;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

public class PlayerConversation implements Conversation {

    private static final int MESSAGE_LIMIT = 100;

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

        while (!messages.isEmpty() && (messages.size() > MESSAGE_LIMIT || !(messages.getFirst() instanceof PlayerRequestMessage))) {
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
