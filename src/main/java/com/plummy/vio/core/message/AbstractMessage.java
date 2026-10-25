package com.plummy.vio.core.message;

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
