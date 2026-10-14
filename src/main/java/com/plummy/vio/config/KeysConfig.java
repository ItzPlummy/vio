package com.plummy.vio.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class KeysConfig {

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.ConfigValue<String> OPENAI;
    public static final ModConfigSpec.ConfigValue<String> ANTHROPIC;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        OPENAI = builder
                .comment("API key for OpenAI provider")
                .define("openai", "");

        ANTHROPIC = builder
                .comment("API key for Anthropic provider")
                .define("anthropic", "");

        SPEC = builder.build();
    }
}
