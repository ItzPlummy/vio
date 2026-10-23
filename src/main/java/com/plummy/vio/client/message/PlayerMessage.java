package com.plummy.vio.client.message;

import java.time.Instant;

public class PlayerMessage extends AbstractClientMessage {

    private final boolean voice;

    public PlayerMessage(String text, Instant time, boolean voice) {
        super(text, time);
        this.voice = voice;
    }

    @Override
    public ClientMessageType getType() {
        return ClientMessageType.PLAYER;
    }

    // Spoken and transcribed, rather than typed.
    public boolean isVoice() {
        return voice;
    }
}
