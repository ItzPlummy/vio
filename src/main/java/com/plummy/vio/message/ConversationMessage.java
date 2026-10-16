package com.plummy.vio.message;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public interface ConversationMessage extends Message {

    // When the message was created, in UTC. Stored and shown to players, but not sent to the model.
    Instant getTime();

    // The current time at the precision messages keep, so a stored time reads back unchanged.
    static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }
}
