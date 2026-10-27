package com.plummy.vio.message;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public abstract class AbstractMessage implements Message {

    private final Instant dispatchTime;

    public AbstractMessage() {
        this(Instant.now().truncatedTo(ChronoUnit.MILLIS));
    }

    public AbstractMessage(Instant dispatchTime) {
        this.dispatchTime = dispatchTime;
    }

    @Override
    public Instant getDispatchTime() {
        return dispatchTime;
    }
}
