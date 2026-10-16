package com.plummy.vio.client.message;

import java.time.Duration;
import java.time.Instant;

public class AgentMessage extends AbstractClientMessage implements ThoughtMessage {

    private final Duration thoughtDuration;

    public AgentMessage(String text, Instant time, Duration thoughtDuration) {
        super(text, time);
        this.thoughtDuration = thoughtDuration;
    }

    @Override
    public ClientMessageType getType() {
        return ClientMessageType.AGENT;
    }

    @Override
    public Duration getThoughtDuration() {
        return thoughtDuration;
    }
}
