package com.plummy.vio.client.message;

import java.time.Instant;

public abstract class AbstractClientMessage implements ClientMessage {

    private final String text;
    private final Instant time;

    protected AbstractClientMessage(String text, Instant time) {
        this.text = text;
        this.time = time;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public Instant getTime() {
        return time;
    }
}
