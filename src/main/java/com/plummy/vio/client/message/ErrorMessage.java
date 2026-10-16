package com.plummy.vio.client.message;

import java.time.Instant;

public class ErrorMessage extends AbstractClientMessage {

    public ErrorMessage(String text, Instant time) {
        super(text, time);
    }

    @Override
    public ClientMessageType getType() {
        return ClientMessageType.ERROR;
    }
}
