package com.plummy.vio.voice;

import com.plummy.vio.config.VoiceLimits;
import java.util.Arrays;
import java.util.UUID;

// One player's capture: their audio, and when they last spoke. Frames arrive on voice chat's thread while
// everything else happens on the server thread, so all access is synchronized.
public class CaptureSession {

    // Kept around the speech, so the first and last sounds aren't clipped.
    private static final int SPEECH_PADDING = VoiceChat.SAMPLE_RATE / 5;
    private static final int INITIAL_CAPACITY = VoiceChat.SAMPLE_RATE * 5;

    private final UUID playerId;
    private final long startMillis;
    private final long silenceMillis;
    private final long maxMillis;

    private short[] samples = new short[INITIAL_CAPACITY];
    private int length;
    private final int maxLength;

    private int speechStart = -1;
    private int speechEnd;
    private long lastFrameMillis = -1;
    private long lastSpeechMillis = -1;

    private VoiceState state = VoiceState.LISTENING;

    public CaptureSession(UUID playerId, long startMillis, long silenceMillis, long maxMillis) {
        this.playerId = playerId;
        this.startMillis = startMillis;
        this.silenceMillis = silenceMillis;
        this.maxMillis = maxMillis;
        this.maxLength = (int) (maxMillis * VoiceChat.SAMPLE_RATE / 1000);
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public synchronized VoiceState getState() {
        return state;
    }

    public synchronized void setState(VoiceState state) {
        this.state = state;
    }

    public synchronized void accept(short[] frame, long nowMillis) {
        lastFrameMillis = nowMillis;

        // The capture finishes at its limit on the next tick.
        if (length + frame.length > maxLength) {
            return;
        }

        if (length + frame.length > samples.length) {
            samples = Arrays.copyOf(samples, Math.min(maxLength, Math.max(samples.length * 2, length + frame.length)));
        }

        System.arraycopy(frame, 0, samples, length, frame.length);

        if (getLevel(frame) >= VoiceLimits.SPEECH_LEVEL) {
            if (speechStart < 0) {
                speechStart = length;
            }

            lastSpeechMillis = nowMillis;
            speechEnd = length + frame.length;
        }

        length += frame.length;
    }

    public synchronized boolean hasAudio() {
        return lastFrameMillis >= 0;
    }

    public synchronized boolean hasSpeech() {
        return speechStart >= 0;
    }

    public synchronized boolean isWithoutAudio(long nowMillis) {
        return lastFrameMillis < 0 && nowMillis - startMillis >= VoiceLimits.NO_AUDIO_MILLIS;
    }

    public synchronized boolean isFinished(long nowMillis) {
        if (nowMillis - startMillis >= maxMillis) {
            return true;
        }

        if (lastSpeechMillis < 0) {
            return nowMillis - startMillis >= VoiceLimits.NO_SPEECH_MILLIS;
        }

        return nowMillis - lastSpeechMillis >= silenceMillis;
    }

    // The audio from the first speech to the last, without the silence around it: less to transcribe, less to pay
    // for, and nothing for Whisper to imagine words in.
    public synchronized short[] getSpeech() {
        if (speechStart < 0) {
            return new short[0];
        }

        return Arrays.copyOfRange(samples, Math.max(0, speechStart - SPEECH_PADDING), Math.min(length, speechEnd + SPEECH_PADDING));
    }

    private static double getLevel(short[] frame) {
        double sum = 0;

        for (short sample : frame) {
            sum += sample * (double) sample;
        }

        return frame.length == 0 ? 0 : Math.sqrt(sum / frame.length);
    }
}
