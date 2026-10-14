package com.plummy.vio.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public class ServerConfig {

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.ConfigValue<String> PROVIDER;

    public static final ModConfigSpec.ConfigValue<String> OPENAI_MODEL;
    public static final ModConfigSpec.ConfigValue<String> ANTHROPIC_MODEL;
    public static final ModConfigSpec.ConfigValue<String> OLLAMA_MODEL;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        PROVIDER = builder
                .comment("LLM provider, currently supported: [openai, anthropic, ollama]")
                .defineInList("provider", "openai", List.of("openai", "anthropic", "ollama"));

        builder.push("openai");
        OPENAI_MODEL = builder
                .comment("Model ID for OpenAI provider")
                .define("model", "gpt-6-luna");
        builder.pop();

        builder.push("anthropic");
        ANTHROPIC_MODEL = builder
                .comment("Model ID for Anthropic provider")
                .define("model", "claude-haiku-5-5");
        builder.pop();

        builder.push("ollama");
        OLLAMA_MODEL = builder
                .comment("Model ID for Ollama provider")
                .define("model", "qwen3.5:4b");
        builder.pop();

        SPEC = builder.build();
    }
}
