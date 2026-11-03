package com.plummy.vio.config;

public final class Endpoints {

    public static final String ANTHROPIC_URI = "https://api.anthropic.com/v1/messages";
    public static final String ANTHROPIC_VERSION = "2023-06-01";
    public static final String OPENAI_URI = "https://api.openai.com/v1/responses";
    public static final String ELEVENLABS_SPEECH_TO_TEXT_URI = "https://api.elevenlabs.io/v1/speech-to-text";
    public static final String ELEVENLABS_TEXT_TO_SPEECH_URI = "https://api.elevenlabs.io/v1/text-to-speech/";
    public static final String WEB_SEARCH_URL = "https://html.duckduckgo.com/html/";
    public static final String WEB_SEARCH_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36";
    public static final String VOICESTUDIO_TRANSCRIPTIONS_PATH = "/v1/audio/transcriptions";
    public static final String VOICESTUDIO_SPEECH_PATH = "/v1/audio/speech";

    private Endpoints() {
    }
}
