package com.plummy.vio.client.message;

import java.time.Instant;

public class StatusMessage extends AbstractClientMessage {

    public StatusMessage(String text, Instant time) {
        super(text, time);
    }

    @Override
    public ClientMessageType getType() {
        return ClientMessageType.STATUS;
    }
}
