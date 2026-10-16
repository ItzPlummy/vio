package com.plummy.vio.client.message;

import java.time.Instant;

public class PlayerMessage extends AbstractClientMessage {

    public PlayerMessage(String text, Instant time) {
        super(text, time);
    }

    @Override
    public ClientMessageType getType() {
        return ClientMessageType.PLAYER;
    }
}
