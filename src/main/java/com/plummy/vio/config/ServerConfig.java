package com.plummy.vio.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Arrays;

public class ServerConfig {

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.ConfigValue<String> PROVIDER;
    public static final ModConfigSpec.ConfigValue<String> ACCESS_LEVEL;

    public static final ModConfigSpec.ConfigValue<String> OPENAI_MODEL;
    public static final ModConfigSpec.ConfigValue<String> ANTHROPIC_MODEL;
    public static final ModConfigSpec.ConfigValue<String> OLLAMA_MODEL;

    public static final ModConfigSpec.ConfigValue<String> SPEECH_TO_TEXT;
    public static final ModConfigSpec.ConfigValue<String> TEXT_TO_SPEECH;
    public static final ModConfigSpec.DoubleValue SILENCE_SECONDS;
    public static final ModConfigSpec.IntValue MAX_CAPTURE_SECONDS;

    public static final ModConfigSpec.ConfigValue<String> LOCAL_MODEL;
    public static final ModConfigSpec.IntValue LOCAL_THREADS;

    public static final ModConfigSpec.ConfigValue<String> ELEVENLABS_VOICE_ID;
    public static final ModConfigSpec.ConfigValue<String> ELEVENLABS_MODEL;
    public static final ModConfigSpec.ConfigValue<String> ELEVENLABS_TRANSCRIPTION_MODEL;
    public static final ModConfigSpec.DoubleValue ELEVENLABS_LANGUAGE_CONFIDENCE;
    public static final ModConfigSpec.ConfigValue<String> ELEVENLABS_OUTPUT_FORMAT;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        PROVIDER = builder
                .comment("LLM provider, currently supported: [openai, anthropic, ollama]")
                .defineInList("provider", "openai", Arrays.asList("openai", "anthropic", "ollama"));

        ACCESS_LEVEL = builder
                .comment("Default access level for players without their own, set with /vio access: [none, basic, full]")
                .defineInList("access_level", "basic", Arrays.asList("none", "basic", "full"));

        builder.push("openai");
        OPENAI_MODEL = builder
                .comment("Model ID for OpenAI provider")
                .define("model", "gpt-6.1-sol");
        builder.pop();

        builder.push("anthropic");
        ANTHROPIC_MODEL = builder
                .comment("Model ID for Anthropic provider")
                .define("model", "claude-sonnet-5-5");
        builder.pop();

        builder.push("ollama");
        OLLAMA_MODEL = builder
                .comment("Model ID for Ollama provider")
                .define("model", "qwen3.5:4b");
        builder.pop();

        // Voice needs Simple Voice Chat on the server and the player's client.
        builder.push("voice");
        SPEECH_TO_TEXT = builder
                .comment("Speech to text for voice requests, currently supported: [elevenlabs, local]")
                .defineInList("speech_to_text", "elevenlabs", Arrays.asList("elevenlabs", "local"));

        TEXT_TO_SPEECH = builder
                .comment("Text to speech for spoken replies, currently supported: [elevenlabs]")
                .defineInList("text_to_speech", "elevenlabs", Arrays.asList("elevenlabs"));

        SILENCE_SECONDS = builder
                .comment("Seconds of silence after speaking that end a voice request and send it")
                .defineInRange("silence_seconds", 2.5, 0.5, 10);

        // Local Whisper hears at most 30 seconds at once.
        MAX_CAPTURE_SECONDS = builder
                .comment("Longest a voice request can be, in seconds; it's sent when reached")
                .defineInRange("max_capture_seconds", 30, 5, 30);

        builder.push("local");
        LOCAL_MODEL = builder
                .comment("Whisper model for local speech to text, downloaded to config/vio/speech on first start: [whisper-tiny, whisper-base, whisper-small]")
                .defineInList("model", "whisper-base", Arrays.asList("whisper-tiny", "whisper-base", "whisper-small"));

        LOCAL_THREADS = builder
                .comment("CPU threads used by local speech to text")
                .defineInRange("threads", 2, 1, 16);
        builder.pop();

        builder.push("elevenlabs");
        ELEVENLABS_VOICE_ID = builder
                .comment("Voice ID for ElevenLabs text to speech")
                .define("voice_id", "bIHbv24MWmeRgasZH58o");

        ELEVENLABS_MODEL = builder
                .comment("Model ID for ElevenLabs text to speech")
                .define("model", "eleven_flash_v2_5");

        ELEVENLABS_TRANSCRIPTION_MODEL = builder
                .comment("Model ID for ElevenLabs speech to text")
                .define("transcription_model", "scribe_v2");

        ELEVENLABS_LANGUAGE_CONFIDENCE = builder
                .comment("Below this confidence in the detected language, speech is transcribed again as the player's game language")
                .defineInRange("language_confidence", 0.8, 0.0, 1.0);

        ELEVENLABS_OUTPUT_FORMAT = builder
                .comment("Audio format requested from ElevenLabs, resampled to 48 kHz if needed: [pcm_16000, pcm_22050, pcm_24000, pcm_48000]")
                .defineInList("output_format", "pcm_48000", Arrays.asList("pcm_16000", "pcm_22050", "pcm_24000", "pcm_48000"));
        builder.pop();
        builder.pop();

        SPEC = builder.build();
    }
}
