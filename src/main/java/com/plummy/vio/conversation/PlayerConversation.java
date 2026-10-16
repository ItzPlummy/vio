package com.plummy.vio.conversation;

import com.plummy.vio.message.ConversationMessage;
import com.plummy.vio.message.PlayerRequestMessage;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

public class PlayerConversation implements Conversation {

    private static final int MESSAGE_LIMIT = 100;

    private final UUID playerId;
    private final Deque<ConversationMessage> messages = new ArrayDeque<>();

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
        return List.copyOf(messages);
    }

    @Override
    public void addMessage(ConversationMessage message) {
        messages.add(message);

        // Trim whole turns, so the history never starts with a tool call, tool output or answer whose player request was dropped.
        while (!messages.isEmpty() && (messages.size() > MESSAGE_LIMIT || !(messages.getFirst() instanceof PlayerRequestMessage))) {
            messages.removeFirst();
        }
    }

    @Override
    public <T> T accept(ConversationVisitor<T> visitor) {
        return visitor.visitPlayerConversation(this);
    }
}
