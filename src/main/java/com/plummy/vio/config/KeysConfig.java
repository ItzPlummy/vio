package com.plummy.vio.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class KeysConfig {

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.ConfigValue<String> OPENAI;
    public static final ModConfigSpec.ConfigValue<String> ANTHROPIC;
    public static final ModConfigSpec.ConfigValue<String> ELEVENLABS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        OPENAI = builder
                .comment("OpenAI API key. Must be set if OpenAI is chosen as the provider in server config")
                .define("openai", "");

        ANTHROPIC = builder
                .comment("Anthropic API key. Must be set if Anthropic is chosen as the provider in server config")
                .define("anthropic", "");

        ELEVENLABS = builder
                .comment("ElevenLabs API key. Must be set if ElevenLabs is chosen as STT or TTS in server config")
                .define("elevenlabs", "");

        SPEC = builder.build();
    }
}
