package com.plummy.vio.provider;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.plummy.vio.Vio;
import com.plummy.vio.conversation.Conversation;
import com.plummy.vio.message.AgentResponseMessage;
import com.plummy.vio.message.ConversationMessage;
import com.plummy.vio.registries.VisitorRegistry;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class OpenAIAgentProvider implements AgentProvider {

    private static final String OPENAI_URI = "https://api.openai.com/v1/responses";

    private final String model;
    private final String apiKey;

    private final HttpClient httpClient;

    public OpenAIAgentProvider(String model, String apiKey) {
        this.model = model;
        this.apiKey = apiKey;
        this.httpClient = Vio.getHttpClient();
    }

    @Override
    public ConversationMessage request(Conversation conversation) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OPENAI_URI))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(buildRequestBody(conversation).toString()))
                .build();

        HttpResponse<String> response;
        try {
            response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new AgentProviderException("OpenAI request failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AgentProviderException("OpenAI request interrupted", e);
        }

        if (response.statusCode() != 200) {
            throw new AgentProviderException("OpenAI returned " + response.statusCode() + ": " + response.body());
        }

        try {
            return parseResponseBody(JsonParser.parseString(response.body()).getAsJsonObject());
        } catch (JsonParseException | IllegalStateException | NullPointerException e) {
            throw new AgentProviderException("Malformed OpenAI response: " + response.body(), e);
        }
    }

    private JsonObject buildRequestBody(Conversation conversation) {
        JsonObject json = new JsonObject();

        JsonArray input = new JsonArray();
        for (ConversationMessage message : conversation.getMessages()) {
            input.add(message.accept(VisitorRegistry.OPENAI_MESSAGE_JSON_SERIALIZER));
        }

        JsonObject reasoning = new JsonObject();
        reasoning.addProperty("effort", "low");

        json.addProperty("model", getModel());
        json.addProperty("instructions", conversation.getSystemMessage().getText().getText());
        json.add("input", input);

        json.addProperty("store", false);
        json.add("reasoning", reasoning);
        json.addProperty("max_output_tokens", 2048);

        return json;
    }

    private ConversationMessage parseResponseBody(JsonObject json) {
        String status = json.get("status").getAsString();

        if (!status.equals("completed")) {
            throw new AgentProviderException("OpenAI response " + status + ": " + json.get("incomplete_details"));
        }

        StringBuilder text = new StringBuilder();

        for (JsonElement element : json.getAsJsonArray("output")) {
            JsonObject item = element.getAsJsonObject();

            if (!item.get("type").getAsString().equals("message")) {
                continue;
            }

            for (JsonElement contentElement : item.getAsJsonArray("content")) {
                JsonObject content = contentElement.getAsJsonObject();

                switch (content.get("type").getAsString()) {
                    case "output_text" -> text.append(content.get("text").getAsString());
                    case "refusal" -> text.append(content.get("refusal").getAsString());
                }
            }
        }

        return new AgentResponseMessage(text.toString());
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
}
