package com.plummy.vio.config;

public final class Endpoints {

    public static final String ANTHROPIC_URI = "https://api.anthropic.com/v1/messages";
    public static final String ANTHROPIC_VERSION = "2023-06-01";
    public static final String OPENAI_URI = "https://api.openai.com/v1/responses";
    public static final String ELEVENLABS_SPEECH_TO_TEXT_URI = "https://api.elevenlabs.io/v1/speech-to-text";
    public static final String ELEVENLABS_TEXT_TO_SPEECH_URI = "https://api.elevenlabs.io/v1/text-to-speech/";
    public static final String WEB_SEARCH_URL = "https://html.duckduckgo.com/html/";
    public static final String WEB_SEARCH_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36";
    public static final String SHERPA_ONNX_VERSION = "1.13.8";
    public static final String SHERPA_ONNX_NATIVE_URI = "https://github.com/k2-fsa/sherpa-onnx/releases/download/v%1$s/sherpa-onnx-native-lib-%2$s-%1$s.jar";
    public static final String PIPER_REPOSITORY_URI = "https://huggingface.co/csukuangfj/vits-piper-%s/resolve/%s/%s";
    public static final String WHISPER_REPOSITORY_URI = "https://huggingface.co/csukuangfj/sherpa-onnx-whisper-%s/resolve/%s/%s";
    public static final String WHISPER_TOKENS_SHA256 = "b34b360dbb493e781e479794586d661700670d65564001f23024971d1f2fa126";

    private Endpoints() {
    }
}
