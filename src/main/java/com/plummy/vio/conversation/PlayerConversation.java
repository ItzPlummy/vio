package com.plummy.vio.conversation;

import com.plummy.vio.config.AgentLimits;
import com.plummy.vio.message.*;

import java.util.*;

public final class PlayerConversation implements Conversation<UUID> {

    private final UUID id;
    private final List<Message> messages = new ArrayList<>();

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

        if (messages.getLast() instanceof AgentResponseMessage) {
            clearOldMessages();
        }
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
        long requests = messages.stream().filter(PlayerRequestMessage.class::isInstance).count();

        int requestsLeft = requests > AgentLimits.MAX_CONVERSATION_REQUESTS ? AgentLimits.TRIMMED_CONVERSATION_REQUESTS : AgentLimits.MAX_CONVERSATION_REQUESTS;
        int toolRequestsLeft = AgentLimits.MAX_CONVERSATION_TOOL_REQUESTS;

        List<Message> kept = new ArrayList<>();

        boolean decided = false;
        boolean keepsTools = false;

        for (Message message : messages.reversed()) {
            if (requestsLeft <= 0) {
                break;
            }

            if (message instanceof ToolRequestMessage || message instanceof ToolResponseMessage) {
                if (!decided) {
                    keepsTools = toolRequestsLeft > 0;
                    toolRequestsLeft--;
                    decided = true;
                }

                if (!keepsTools) {
                    continue;
                }
            }

            if (message instanceof PlayerRequestMessage) {
                requestsLeft--;
                decided = false;
            }

            kept.add(message);
        }

        messages.clear();
        messages.addAll(kept.reversed());
    }
}
