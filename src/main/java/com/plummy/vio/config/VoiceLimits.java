package com.plummy.vio.config;

import java.time.Duration;

public final class VoiceLimits {

    public static final double SPEECH_LEVEL = 400;
    public static final long NO_AUDIO_MILLIS = 1000;
    public static final long NO_SPEECH_MILLIS = 5000;
    public static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private VoiceLimits() {
    }
}
