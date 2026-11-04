package com.plummy.vio.core.conversation;

import com.plummy.vio.core.turn.Turn;
import com.plummy.vio.core.user.PlayerUser;

import java.util.Collection;
import java.util.UUID;

public final class PlayerConversation extends AbstractConversation<PlayerUser> implements Conversation<PlayerUser> {

    private final UUID id;

    public PlayerConversation(UUID id) {
        this.id = id;
    }

    public PlayerConversation(UUID id, Collection<Turn<PlayerUser>> turns) {
        super(turns);
        this.id = id;
    }

    @Override
    public <V> V accept(ConversationVisitor<V> visitor) {
        return visitor.visit(this);
    }

    public UUID getId() {
        return id;
    }
}
