package com.plummy.vio.config;

import com.plummy.vio.Vio;
import net.neoforged.neoforge.common.ModConfigSpec;

public class KeysConfig {

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.ConfigValue<String> OPENAI;
    public static final ModConfigSpec.ConfigValue<String> ANTHROPIC;
    public static final ModConfigSpec.ConfigValue<String> ELEVENLABS;
    public static final ModConfigSpec.ConfigValue<String> VOICESTUDIO;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        OPENAI = builder
                .comment("OpenAI API key. Must be set if OpenAI is chosen as the provider in server config")
                .translation(key("openai"))
                .define("openai", "");

        ANTHROPIC = builder
                .comment("Anthropic API key. Must be set if Anthropic is chosen as the provider in server config")
                .translation(key("anthropic"))
                .define("anthropic", "");

        ELEVENLABS = builder
                .comment("ElevenLabs API key. Must be set if ElevenLabs is chosen as STT or TTS in server config")
                .translation(key("elevenlabs"))
                .define("elevenlabs", "");

        VOICESTUDIO = builder
                .comment("VoiceStudio API key. Only needed if VoiceStudio runs on another machine")
                .translation(key("voicestudio"))
                .define("voicestudio", "");

        SPEC = builder.build();
    }

    private static String key(String path) {
        return "%s.configuration.keys.%s".formatted(Vio.MODID, path);
    }
}
