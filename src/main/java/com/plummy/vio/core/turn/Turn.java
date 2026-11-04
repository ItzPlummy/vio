package com.plummy.vio.core.turn;

import com.plummy.vio.core.user.User;

import java.time.Instant;
import java.util.List;

public sealed interface Turn<T extends User> permits PlayerTurn {

    T getUser();

    InputType getInputType();

    TurnStatus getStatus();

    String getRequest();

    String getResponse();

    List<ToolExecution> getToolExecutions();

    Instant getTurnStartTime();

    Instant getTurnEndTime();

    void complete(String response);

    void addToolExecution(ToolExecution toolExecution);

    <V> V accept(TurnVisitor<V> visitor);

    default boolean isCompleted() {
        return getStatus() == TurnStatus.COMPLETED;
    }
}
