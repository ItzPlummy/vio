package com.plummy.vio.core.turn;

import com.plummy.vio.core.user.User;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

abstract class AbstractTurn<T extends User> {

    private final T user;
    private final InputType inputType;
    private TurnStatus status;
    private final String request;
    private String response;
    private final List<ToolExecution> toolExecutions;
    private final Instant turnStartTime;
    private Instant turnEndTime;

    AbstractTurn(T user, InputType inputType, String request) {
        this.user = user;
        this.inputType = inputType;
        this.status = TurnStatus.PENDING;
        this.request = request;
        this.toolExecutions = new ArrayList<>();
        this.turnStartTime = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    AbstractTurn(T user, InputType inputType, TurnStatus status, String request, String response, List<ToolExecution> toolExecutions, Instant turnStartTime, Instant turnEndTime) {
        this.user = user;
        this.inputType = inputType;
        this.status = status;
        this.request = request;
        this.response = response;
        this.toolExecutions = new ArrayList<>(toolExecutions);
        this.turnStartTime = turnStartTime;
        this.turnEndTime = turnEndTime;
    }

    public T getUser() {
        return user;
    }

    public InputType getInputType() {
        return inputType;
    }

    public synchronized TurnStatus getStatus() {
        return status;
    }

    public String getRequest() {
        return request;
    }

    public synchronized String getResponse() {
        return response;
    }

    public synchronized List<ToolExecution> getToolExecutions() {
        return List.copyOf(toolExecutions);
    }

    public Instant getTurnStartTime() {
        return turnStartTime;
    }

    public synchronized Instant getTurnEndTime() {
        return turnEndTime;
    }

    public synchronized void complete(String response) {
        if (status == TurnStatus.COMPLETED) {
            throw new IllegalStateException("Cannot complete a completed turn");
        }

        this.status = TurnStatus.COMPLETED;
        this.response = response;
        this.turnEndTime = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    public synchronized void addToolExecution(ToolExecution toolExecution) {
        if (status == TurnStatus.COMPLETED) {
            throw new IllegalStateException("Cannot add tool execution to a completed turn");
        }

        toolExecutions.add(toolExecution);
    }
}
