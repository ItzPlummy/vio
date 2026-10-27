package com.plummy.vio.message;

import java.time.Instant;

public abstract class AbstractMessage implements Message {

    private final Instant time;

    public AbstractMessage(Instant time) {
        this.time = time;
    }

    @Override
    public Instant getTime() {
        return time;
    }
}
