package com.plummy.vio.conversation;

import com.plummy.vio.config.AgentLimits;
import com.plummy.vio.message.Message;
import com.plummy.vio.message.PlayerRequestMessage;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

public final class PlayerConversation implements Conversation<UUID> {

    private final UUID id;
    private final Deque<Message> messages = new ArrayDeque<>();

    public PlayerConversation(UUID id) {
        this.id = id;
    }

    @Override
    public synchronized List<Message> getMessages() {
        return List.copyOf(messages);
    }

    @Override
    public synchronized void addMessage(Message message) {
        messages.add(message);
        clearOldMessages();
    }

    @Override
    public <T> T accept(ConversationVisitor<T> visitor) {
        return visitor.visitPlayerConversation(this);
    }

    @Override
    public UUID getKey() {
        return getId();
    }

    public UUID getId() {
        return id;
    }

    private synchronized void clearOldMessages() {
        while (!messages.isEmpty() && (messages.size() > AgentLimits.MAX_CONVERSATION_MESSAGES || !(messages.getFirst() instanceof PlayerRequestMessage))) {
            messages.removeFirst();
        }
    }
}
