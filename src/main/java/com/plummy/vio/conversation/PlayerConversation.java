package com.plummy.vio.conversation;

import java.util.UUID;

public class PlayerConversation extends AbstractConversation<UUID> {

    private final UUID playerId;

    public PlayerConversation(UUID playerId) {
        this.playerId = playerId;
    }

    @Override
    public <T> T accept(ConversationVisitor<T> visitor) {
        return visitor.visitPlayerConversation(this);
    }

    @Override
    public UUID getKey() {
        return getPlayerId();
    }

    public UUID getPlayerId() {
        return playerId;
    }
}
