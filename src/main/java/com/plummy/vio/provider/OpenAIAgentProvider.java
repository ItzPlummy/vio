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
import com.plummy.vio.message.ToolRequestMessage;
import com.plummy.vio.registry.ToolRegistry;
import com.plummy.vio.registry.VisitorRegistry;
import com.plummy.vio.section.TextSection;
import com.plummy.vio.tool.Tool;
import com.plummy.vio.tool.ToolArgumentException;
import com.plummy.vio.tool.ToolArguments;
import com.plummy.vio.tool.ToolExecutionRequest;
import com.plummy.vio.tool.ToolArgumentDefinition;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SequencedMap;

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
        } catch (JsonParseException | IllegalStateException | NullPointerException | ToolArgumentException e) {
            throw new AgentProviderException("Malformed OpenAI response: " + response.body(), e);
        }
    }

    private JsonObject buildRequestBody(Conversation conversation) {
        JsonObject json = new JsonObject();

        JsonArray input = new JsonArray();
        for (ConversationMessage message : conversation.getMessages()) {
            message.accept(VisitorRegistry.OPENAI_MESSAGE_JSON_SERIALIZER).forEach(input::add);
        }

        JsonArray tools = new JsonArray();
        for (Tool tool : ToolRegistry.getAll()) {
            tools.add(buildToolDefinition(tool));
        }

        JsonObject reasoning = new JsonObject();
        reasoning.addProperty("effort", "low");

        json.addProperty("model", getModel());
        json.addProperty("instructions", conversation.getSystemMessage().getText().getText());
        json.add("input", input);
        json.add("tools", tools);

        json.addProperty("store", false);
        json.add("reasoning", reasoning);
        json.addProperty("max_output_tokens", 2048);

        return json;
    }

    private static JsonObject buildToolDefinition(Tool tool) {
        JsonObject properties = new JsonObject();
        JsonArray required = new JsonArray();

        for (Map.Entry<String, ToolArgumentDefinition> entry : tool.getArgumentSchema().entrySet()) {
            JsonObject property = new JsonObject();

            property.addProperty("type", entry.getValue().getType().getValue());
            property.addProperty("description", entry.getValue().getDescription());

            properties.add(entry.getKey(), property);
            required.add(entry.getKey());
        }

        JsonObject parameters = new JsonObject();

        parameters.addProperty("type", "object");
        parameters.add("properties", properties);
        parameters.add("required", required);
        parameters.addProperty("additionalProperties", false);

        JsonObject json = new JsonObject();

        json.addProperty("type", "function");
        json.addProperty("name", tool.getName());
        json.addProperty("description", tool.getDescription());
        json.add("parameters", parameters);
        json.addProperty("strict", true);

        return json;
    }

    private ConversationMessage parseResponseBody(JsonObject json) {
        String status = json.get("status").getAsString();

        if (!status.equals("completed")) {
            throw new AgentProviderException("OpenAI response " + status + ": " + json.get("incomplete_details"));
        }

        StringBuilder text = new StringBuilder();
        SequencedMap<String, ToolExecutionRequest> toolExecutionRequests = new LinkedHashMap<>();

        for (JsonElement element : json.getAsJsonArray("output")) {
            JsonObject item = element.getAsJsonObject();

            switch (item.get("type").getAsString()) {
                case "message" -> {
                    for (JsonElement contentElement : item.getAsJsonArray("content")) {
                        JsonObject content = contentElement.getAsJsonObject();

                        switch (content.get("type").getAsString()) {
                            case "output_text" -> text.append(content.get("text").getAsString());
                            case "refusal" -> text.append(content.get("refusal").getAsString());
                        }
                    }
                }
                case "function_call" -> toolExecutionRequests.put(
                        item.get("call_id").getAsString(),

                        new ToolExecutionRequest(
                                item.get("name").getAsString(),
                                ToolArguments.parse(item.get("arguments").getAsString())
                        )
                );
            }
        }

        if (toolExecutionRequests.isEmpty()) {
            return new AgentResponseMessage(text.toString());
        }

        return new ToolRequestMessage(new TextSection(text.toString()), toolExecutionRequests);
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
