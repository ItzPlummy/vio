package com.plummy.vio.conversation;

import com.plummy.vio.config.AgentLimits;
import com.plummy.vio.message.Message;
import com.plummy.vio.message.PlayerRequestMessage;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public abstract class AbstractConversation<K> implements Conversation<K> {

    private final Deque<Message> messages = new ArrayDeque<>();

    @Override
    public synchronized List<Message> getMessages() {
        return List.copyOf(messages);
    }

    @Override
    public synchronized void addMessage(Message message) {
        messages.add(message);
        clearOldMessages();
    }

    protected synchronized void clearOldMessages() {
        while (!messages.isEmpty() && (messages.size() > AgentLimits.MAX_CONVERSATION_MESSAGES || !(messages.getFirst() instanceof PlayerRequestMessage))) {
            messages.removeFirst();
        }
    }
}
