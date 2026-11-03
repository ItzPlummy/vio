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
                    ServerConfig.ELEVENLABS_TEXT_TO_SPEECH_MODEL.get(),
                    ServerConfig.ELEVENLABS_OUTPUT_FORMAT.get(),
                    KeysConfig.ELEVENLABS.get()
            );
            case "voicestudio" -> new VoiceStudioTextToSpeechTransformer(
                    ServerConfig.VOICESTUDIO_URI.get(),
                    ServerConfig.VOICESTUDIO_MODEL.get(),
                    ServerConfig.VOICESTUDIO_VOICE.get(),
                    KeysConfig.VOICESTUDIO.get()
            );
            default -> throw new IllegalArgumentException("Text to speech '%s' isn't available".formatted(ServerConfig.TEXT_TO_SPEECH.get()));
        };
    }
}
