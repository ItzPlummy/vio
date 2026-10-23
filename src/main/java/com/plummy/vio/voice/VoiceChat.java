package com.plummy.vio.voice;

import java.util.UUID;
import java.util.function.Consumer;

// What Vio needs from a voice chat mod. Implemented in voice.svc, the only package that may reference Simple Voice Chat.
public interface VoiceChat {

    // Simple Voice Chat's audio is 16-bit mono at this rate, and so is all audio Vio passes around.
    int SAMPLE_RATE = 48_000;

    VoiceAvailability getAvailability(UUID playerId);

    // Hands each decoded microphone frame to `frames`, on voice chat's own thread, until stopListening.
    void startListening(UUID playerId, Consumer<short[]> frames);

    void stopListening(UUID playerId);

    // Only that player hears it. `onStopped` runs on voice chat's own thread once it ends or is stopped.
    void play(UUID playerId, short[] samples, Runnable onStopped);

    void stopPlaying(UUID playerId);
}
