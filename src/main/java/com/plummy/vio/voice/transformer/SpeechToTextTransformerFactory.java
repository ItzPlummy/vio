package com.plummy.vio.voice.transformer;

import com.plummy.vio.config.KeysConfig;
import com.plummy.vio.config.ServerConfig;

public class SpeechToTextTransformerFactory {

    private SpeechToTextTransformerFactory() {
    }

    public static SpeechToTextTransformer createTransformer() {
        return switch (ServerConfig.SPEECH_TO_TEXT.get()) {
            case "elevenlabs" -> new ElevenLabsSpeechToTextTransformer(ServerConfig.ELEVENLABS_TRANSCRIPTION_MODEL.get(), ServerConfig.ELEVENLABS_LANGUAGE_CONFIDENCE.get(), KeysConfig.ELEVENLABS.get());
            case "local" -> new LocalSpeechToTextTransformer(WhisperModel.fromValue(ServerConfig.LOCAL_MODEL.get()), ServerConfig.LOCAL_THREADS.get());
            default -> throw new IllegalArgumentException("Speech to text '" + ServerConfig.SPEECH_TO_TEXT.get() + "' is not available");
        };
    }
}
