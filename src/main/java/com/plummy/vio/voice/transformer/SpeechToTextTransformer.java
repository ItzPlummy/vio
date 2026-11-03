package com.plummy.vio.voice.transformer;

public interface SpeechToTextTransformer {

    // Samples are 16-bit mono at VoiceChat.SAMPLE_RATE. The game language (ISO 639-1, or null) may settle speech
    // whose own language isn't clear. Blocking, so never called on the server thread.
    Transcript transcribe(short[] samples, String gameLanguage);
}
