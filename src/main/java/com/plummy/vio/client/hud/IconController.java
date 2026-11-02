package com.plummy.vio.client.hud;

import com.plummy.vio.client.conversation.ClientConversation;
import com.plummy.vio.voice.VoiceState;
import net.minecraft.Util;

public class IconController {

    private static final IconController INSTANCE = new IconController();

    // The pulse holds between frames, stepping like a sprite instead of moving smoothly.
    private static final long FRAME_MILLIS = 50;

    private static final float GROW_EASING = 6f;
    private static final float PULSE_EASING = 2.5f;
    private static final float BUSY_PULSE_SPEED = 3f;
    private static final float MAX_FRAME_SECONDS = 0.1f;

    private float grow;
    private float pulseSpeed = 1;

    // Advanced by the eased speed, so speeding up doesn't make the rings jump.
    private double pulseSeconds;
    private double framePulseSeconds;
    private double frameSeconds;
    private long frame = -1;

    private long lastNanos = Util.getNanos();

    public static IconController getInstance() {
        return INSTANCE;
    }

    private IconController() {
    }

    public void update() {
        long nanos = Util.getNanos();
        float seconds = Math.min((nanos - lastNanos) / 1.0E9f, MAX_FRAME_SECONDS);
        lastNanos = nanos;

        ClientConversation conversation = ClientConversation.getInstance();
        VoiceState voiceState = conversation.getVoiceState();
        boolean recording = conversation.isCapturing();

        // Big while it's the player's turn to talk or Vio's, and back to normal while the reply is worked on.
        grow = ease(grow, recording || voiceState == VoiceState.TRANSCRIBING || voiceState == VoiceState.SPEAKING ? 1 : 0, GROW_EASING, seconds);
        pulseSpeed = ease(pulseSpeed, conversation.isBusy() ? BUSY_PULSE_SPEED : 1, PULSE_EASING, seconds);
        pulseSeconds += pulseSpeed * seconds;

        long millis = Util.getMillis();

        if (millis / FRAME_MILLIS != frame) {
            frame = millis / FRAME_MILLIS;
            frameSeconds = frame * FRAME_MILLIS / 1000.0;
            framePulseSeconds = pulseSeconds;
        }
    }

    public float getGrow() {
        return grow;
    }

    // Seconds of pulse at its normal speed, stepped to the frame.
    public double getPulseSeconds() {
        return framePulseSeconds;
    }

    // Clock seconds, stepped to the frame.
    public double getFrameSeconds() {
        return frameSeconds;
    }

    private static float ease(float current, float target, float rate, float seconds) {
        return current + (target - current) * (1 - (float) Math.exp(-rate * seconds));
    }
}
