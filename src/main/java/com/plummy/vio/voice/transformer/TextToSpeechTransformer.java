package com.plummy.vio.voice.transformer;

public interface TextToSpeechTransformer {

    // Returns 16-bit mono samples at VoiceChat.SAMPLE_RATE. The language (ISO 639-1, or null to let the transformer
    // decide) is the one the text is in. Blocking, so never called on the server thread.
    short[] synthesize(String text, String language);
}
