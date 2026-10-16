package com.plummy.vio.client.message;

import java.time.Instant;

public interface ClientMessage {

    ClientMessageType getType();

    String getText();

    Instant getTime();
}
