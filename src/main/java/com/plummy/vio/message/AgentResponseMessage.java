package com.plummy.vio.message;

import java.time.Instant;

public class AgentResponseMessage extends AbstractMessage {

    private final String response;

    public AgentResponseMessage(String response) {
        this.response = response;
    }

    public AgentResponseMessage(String response, Instant dispatchTime) {
        super(dispatchTime);

        this.response = response;
    }

    @Override
    public <T> T accept(MessageVisitor<T> visitor) {
        return visitor.visitAgentResponseMessage(this);
    }

    public String getResponse() {
        return response;
    }
}
