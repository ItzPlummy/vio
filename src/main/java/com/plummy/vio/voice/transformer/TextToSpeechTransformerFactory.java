package com.plummy.vio.voice.transformer;

import com.plummy.vio.config.KeysConfig;
import com.plummy.vio.config.ServerConfig;

public class TextToSpeechTransformerFactory {

    private TextToSpeechTransformerFactory() {
    }

    public static TextToSpeechTransformer createTransformer() {
        return switch (ServerConfig.TEXT_TO_SPEECH.get()) {
            case "elevenlabs" -> new ElevenLabsTextToSpeechTransformer(
                    ServerConfig.ELEVENLABS_VOICE_ID.get(),
                    ServerConfig.ELEVENLABS_MODEL.get(),
                    ServerConfig.ELEVENLABS_OUTPUT_FORMAT.get(),
                    KeysConfig.ELEVENLABS.get()
            );
            default -> throw new IllegalArgumentException("Text to speech '" + ServerConfig.TEXT_TO_SPEECH.get() + "' is not available");
        };
    }
}
