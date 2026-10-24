package com.plummy.vio.provider;

import com.plummy.vio.config.KeysConfig;
import com.plummy.vio.config.ServerConfig;

public class ProviderFactory {

    private ProviderFactory() {
    }

    public static Provider createProvider() {
        return switch (ServerConfig.PROVIDER.get()) {
            case "openai" -> new OpenAIProvider(ServerConfig.OPENAI_MODEL.get(), KeysConfig.OPENAI.get());
            case "anthropic" -> new AnthropicProvider(ServerConfig.ANTHROPIC_MODEL.get(), KeysConfig.ANTHROPIC.get());
            case "ollama" -> new OllamaProvider(ServerConfig.OLLAMA_MODEL.get(), ServerConfig.OLLAMA_CONTEXT_LENGTH.get());
            default -> throw new IllegalArgumentException("Provider '" + ServerConfig.PROVIDER.get() + "' is not available");
        };
    }
}
