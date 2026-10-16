package com.plummy.vio.provider;

import com.plummy.vio.Vio;
import com.plummy.vio.conversation.Conversation;
import com.plummy.vio.message.ConversationMessage;
import com.plummy.vio.message.SystemMessage;
import com.plummy.vio.tool.Tool;

import java.net.http.HttpClient;
import java.util.Collection;

public class AnthropicProvider implements Provider {

    private final String model;
    private final String apiKey;
    private final HttpClient httpClient;

    public AnthropicProvider(String model, String apiKey) {
        this.model = model;
        this.apiKey = apiKey;
        this.httpClient = Vio.getHttpClient();
    }

    public String getModel() {
        return model;
    }

    public String getApiKey() {
        return apiKey;
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    @Override
    public ConversationMessage request(SystemMessage systemMessage, Conversation conversation) {
        return null;
    }
}
