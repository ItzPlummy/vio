package com.plummy.vio.message;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public final class AgentResponseMessage implements Message {

    private final String response;
    private final Instant dispatchTime;

    public AgentResponseMessage(String response) {
        this.response = response;
        this.dispatchTime = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    public AgentResponseMessage(String response, Instant dispatchTime) {
        this.response = response;
        this.dispatchTime = dispatchTime;
    }

    @Override
    public Instant getDispatchTime() {
        return dispatchTime;
    }

    @Override
    public <T> T accept(MessageVisitor<T> visitor) {
        return visitor.visitAgentResponseMessage(this);
    }

    public String getResponse() {
        return response;
    }
}
