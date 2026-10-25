package com.plummy.vio.voice.transformer;

import com.plummy.vio.config.KeysConfig;
import com.plummy.vio.config.ServerConfig;

public class SpeechToTextTransformerFactory {

    private SpeechToTextTransformerFactory() {
    }

    public static SpeechToTextTransformer createTransformer() {
        return switch (ServerConfig.SPEECH_TO_TEXT.get()) {
            case "elevenlabs" -> new ElevenLabsSpeechToTextTransformer(ServerConfig.ELEVENLABS_SPEECH_TO_TEXT_MODEL.get(), ServerConfig.ELEVENLABS_LANGUAGE_CONFIDENCE.get(), KeysConfig.ELEVENLABS.get());
            case "local" -> new LocalSpeechToTextTransformer(WhisperModel.fromValue(ServerConfig.LOCAL_MODEL.get()), 4);
            default -> throw new IllegalArgumentException("Speech to text '%s' isn't available".formatted(ServerConfig.SPEECH_TO_TEXT.get()));
        };
    }
}
