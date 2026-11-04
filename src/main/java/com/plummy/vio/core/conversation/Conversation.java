package com.plummy.vio.core.conversation;

import com.plummy.vio.core.turn.Turn;
import com.plummy.vio.core.user.User;

import java.util.List;

public sealed interface Conversation<T extends User> permits PlayerConversation {

    Turn<T> getActiveTurn();

    List<Turn<T>> getTurns();

    void beginTurn(Turn<T> turn);

    <V> V accept(ConversationVisitor<V> visitor);

    default boolean isActive() {
        return getActiveTurn() != null && !getActiveTurn().isCompleted();
    }
}
