package com.plummy.vio.message;

import com.plummy.vio.section.Section;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

public interface Message {

    MessageType getType();

    List<Section> getSections();

    // When the message was created, in UTC. Stored and shown to players, but not sent to the model.
    Instant getTime();

    <T> T accept(MessageVisitor<T> visitor);

    // The current time at the precision messages keep, so a stored time reads back unchanged.
    static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }
}
