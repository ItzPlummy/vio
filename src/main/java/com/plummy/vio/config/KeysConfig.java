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
                .comment("API key for OpenAI provider")
                .define("openai", "");

        ANTHROPIC = builder
                .comment("API key for Anthropic provider")
                .define("anthropic", "");

        ELEVENLABS = builder
                .comment("API key for ElevenLabs speech to text and text to speech")
                .define("elevenlabs", "");

        SPEC = builder.build();
    }
}
