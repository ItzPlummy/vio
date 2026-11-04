package com.plummy.vio.core.turn;

public interface TurnVisitor<T> {

    T visit(PlayerTurn turn);
}
