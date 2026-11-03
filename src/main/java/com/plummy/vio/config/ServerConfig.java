package com.plummy.vio.config;

import com.plummy.vio.Vio;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Arrays;

public class ServerConfig {

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.ConfigValue<String> PROVIDER;
    public static final ModConfigSpec.ConfigValue<String> DEFAULT_ACCESS_LEVEL;

    public static final ModConfigSpec.ConfigValue<String> OPENAI_MODEL;
    public static final ModConfigSpec.ConfigValue<String> ANTHROPIC_MODEL;
    public static final ModConfigSpec.ConfigValue<String> OLLAMA_URI;
    public static final ModConfigSpec.ConfigValue<String> OLLAMA_MODEL;
    public static final ModConfigSpec.IntValue OLLAMA_CONTEXT_LENGTH;

    public static final ModConfigSpec.ConfigValue<String> SPEECH_TO_TEXT;
    public static final ModConfigSpec.ConfigValue<String> TEXT_TO_SPEECH;
    public static final ModConfigSpec.DoubleValue SILENCE_SECONDS;
    public static final ModConfigSpec.IntValue MAX_CAPTURE_SECONDS;

    public static final ModConfigSpec.ConfigValue<String> VOICESTUDIO_URI;
    public static final ModConfigSpec.ConfigValue<String> VOICESTUDIO_MODEL;
    public static final ModConfigSpec.ConfigValue<String> VOICESTUDIO_VOICE;

    public static final ModConfigSpec.ConfigValue<String> ELEVENLABS_VOICE_ID;
    public static final ModConfigSpec.ConfigValue<String> ELEVENLABS_TEXT_TO_SPEECH_MODEL;
    public static final ModConfigSpec.ConfigValue<String> ELEVENLABS_SPEECH_TO_TEXT_MODEL;
    public static final ModConfigSpec.DoubleValue ELEVENLABS_LANGUAGE_CONFIDENCE;
    public static final ModConfigSpec.ConfigValue<String> ELEVENLABS_OUTPUT_FORMAT;

    public static final ModConfigSpec.BooleanValue SCRIPTS_ENABLED;
    public static final ModConfigSpec.IntValue RUN_BUDGET_MILLIS;
    public static final ModConfigSpec.IntValue PERIODIC_BUDGET_MILLIS;
    public static final ModConfigSpec.IntValue LISTENER_BUDGET_MILLIS;
    public static final ModConfigSpec.IntValue COMMAND_BUDGET_MILLIS;
    public static final ModConfigSpec.IntValue MAX_FAILURES;

    public static final ModConfigSpec.BooleanValue DATAPACK_ENABLED;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        PROVIDER = builder
                .comment("LLM provider, currently supported: [openai, anthropic, ollama]")
                .translation(key("provider"))
                .defineInList("provider", "openai", Arrays.asList("openai", "anthropic", "ollama"));

        DEFAULT_ACCESS_LEVEL = builder
                .comment("Default access level for players without their own, set with /vio access: [none, basic, full]")
                .translation(key("default_access_level"))
                .defineInList("default_access_level", "basic", Arrays.asList("none", "basic", "full"));

        builder.translation(key("openai")).push("openai");
        OPENAI_MODEL = builder
                .comment("Model ID for OpenAI provider")
                .translation(key("openai.model"))
                .define("model", "gpt-6.1-sol");
        builder.pop();

        builder.translation(key("anthropic")).push("anthropic");
        ANTHROPIC_MODEL = builder
                .comment("Model ID for Anthropic provider")
                .translation(key("anthropic.model"))
                .define("model", "claude-sonnet-5-5");
        builder.pop();

        builder.translation(key("ollama")).push("ollama");
        OLLAMA_URI = builder
                .comment("Chat endpoint of the Ollama server. Use 127.0.0.1 rather than localhost: Ollama listens on IPv4 only, and Java may resolve localhost to ::1 first")
                .translation(key("ollama.uri"))
                .define("uri", "http://127.0.0.1:11434/api/chat");

        OLLAMA_MODEL = builder
                .comment("Model ID for Ollama provider")
                .translation(key("ollama.model"))
                .define("model", "qwen3.5:4b");

        OLLAMA_CONTEXT_LENGTH = builder
                .comment("Context window for Ollama provider, in tokens. Must fit the instructions, tools and conversation plus the reply")
                .translation(key("ollama.context_length"))
                .defineInRange("context_length", 32768, 4096, 262144);
        builder.pop();

        builder.translation(key("voice")).push("voice");
        SPEECH_TO_TEXT = builder
                .comment("Speech to text for voice requests, currently supported: [elevenlabs, voicestudio]")
                .translation(key("voice.speech_to_text"))
                .defineInList("speech_to_text", "elevenlabs", Arrays.asList("elevenlabs", "voicestudio"));

        TEXT_TO_SPEECH = builder
                .comment("Text to speech for spoken replies, currently supported: [elevenlabs, voicestudio]")
                .translation(key("voice.text_to_speech"))
                .defineInList("text_to_speech", "elevenlabs", Arrays.asList("elevenlabs", "voicestudio"));

        SILENCE_SECONDS = builder
                .comment("Seconds of silence after speaking that end a voice request and send it")
                .translation(key("voice.silence_seconds"))
                .defineInRange("silence_seconds", 2.5, 0.5, 10);

        MAX_CAPTURE_SECONDS = builder
                .comment("Longest a voice request can be, in seconds; it's sent when reached")
                .translation(key("voice.max_capture_seconds"))
                .defineInRange("max_capture_seconds", 30, 5, 30);

        builder.translation(key("voice.voicestudio")).push("voicestudio");
        VOICESTUDIO_URI = builder
                .comment("Address of the VoiceStudio server")
                .translation(key("voice.voicestudio.uri"))
                .define("uri", "http://127.0.0.1:3900");

        VOICESTUDIO_MODEL = builder
                .comment("VoiceStudio text to speech engine, e.g. omnivoice or kittentts. The first request after a change waits for it to load")
                .translation(key("voice.voicestudio.model"))
                .define("model", "omnivoice");

        VOICESTUDIO_VOICE = builder
                .comment("VoiceStudio voice profile ID or name, or default for the engine's default voice")
                .translation(key("voice.voicestudio.voice"))
                .define("voice", "default");
        builder.pop();

        builder.translation(key("voice.elevenlabs")).push("elevenlabs");
        ELEVENLABS_VOICE_ID = builder
                .comment("Voice ID for ElevenLabs text to speech")
                .translation(key("voice.elevenlabs.voice_id"))
                .define("voice_id", "bIHbv24MWmeRgasZH58o");

        ELEVENLABS_SPEECH_TO_TEXT_MODEL = builder
                .comment("Model ID for ElevenLabs speech to text")
                .translation(key("voice.elevenlabs.transcription_model"))
                .define("transcription_model", "scribe_v2");

        ELEVENLABS_TEXT_TO_SPEECH_MODEL = builder
                .comment("Model ID for ElevenLabs text to speech")
                .translation(key("voice.elevenlabs.model"))
                .define("model", "eleven_flash_v2_5");

        ELEVENLABS_LANGUAGE_CONFIDENCE = builder
                .comment("Below this confidence in the detected language, speech is transcribed again as the player's game language")
                .translation(key("voice.elevenlabs.language_confidence"))
                .defineInRange("language_confidence", 0.8, 0.0, 1.0);

        ELEVENLABS_OUTPUT_FORMAT = builder
                .comment("Audio format requested from ElevenLabs, resampled to 48 kHz if needed: [pcm_16000, pcm_22050, pcm_24000, pcm_48000]")
                .translation(key("voice.elevenlabs.output_format"))
                .defineInList("output_format", "pcm_48000", Arrays.asList("pcm_16000", "pcm_22050", "pcm_24000", "pcm_48000"));
        builder.pop();
        builder.pop();

        builder.translation(key("scripts")).push("scripts");
        SCRIPTS_ENABLED = builder
                .comment("Whether FULL players can have Vio write and run Groovy scripts on the server. Scripts can do anything the server can, including reading and writing files. Takes effect on restart")
                .translation(key("scripts.enabled"))
                .define("enabled", true);

        RUN_BUDGET_MILLIS = builder
                .comment("Longest a script run started by Vio can take, in milliseconds. One-shot scripts and periodic scripts run on demand")
                .translation(key("scripts.run_budget_millis"))
                .defineInRange("run_budget_millis", 1000, 1, 10000);

        PERIODIC_BUDGET_MILLIS = builder
                .comment("Longest a scheduled run of a periodic script can take, in milliseconds")
                .translation(key("scripts.periodic_budget_millis"))
                .defineInRange("periodic_budget_millis", 20, 1, 1000);

        LISTENER_BUDGET_MILLIS = builder
                .comment("Longest one event handler call of a listener script can take, in milliseconds")
                .translation(key("scripts.listener_budget_millis"))
                .defineInRange("listener_budget_millis", 10, 1, 1000);

        COMMAND_BUDGET_MILLIS = builder
                .comment("Longest one execution of a script command can take, in milliseconds")
                .translation(key("scripts.command_budget_millis"))
                .defineInRange("command_budget_millis", 250, 1, 10000);

        MAX_FAILURES = builder
                .comment("Consecutive failures, timeouts included, after which a script is disabled until it's enabled again or the server restarts")
                .translation(key("scripts.max_failures"))
                .defineInRange("max_failures", 5, 1, 100);
        builder.pop();

        builder.translation(key("datapack")).push("datapack");
        DATAPACK_ENABLED = builder
                .comment("Whether FULL players can have Vio write files into a data pack kept in the world's serverconfig/vio/datapack and reload it. Takes effect on restart; while it's off, the pack isn't loaded, but its files stay on disk")
                .translation(key("datapack.enabled"))
                .define("enabled", true);
        builder.pop();

        SPEC = builder.build();
    }

    private static String key(String path) {
        return "%s.configuration.server.%s".formatted(Vio.MODID, path);
    }
}
