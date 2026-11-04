package com.plummy.vio.core.conversation;

import com.plummy.vio.core.turn.Turn;
import com.plummy.vio.core.user.User;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.List;

abstract class AbstractConversation<T extends User> {

    private final Deque<Turn<T>> turns = new ArrayDeque<>();

    public AbstractConversation() {
    }

    public AbstractConversation(Collection<Turn<T>> turns) {
        this.turns.addAll(turns);
    }

    public synchronized Turn<T> getActiveTurn() {
        return turns.getLast();
    }

    public synchronized List<Turn<T>> getTurns() {
        return List.copyOf(turns);
    }

    public synchronized void beginTurn(Turn<T> turn) {
        turns.addLast(turn);
    }
}
