package com.plummy.vio.provider;

import com.google.gson.*;
import com.plummy.vio.Vio;
import com.plummy.vio.access.AccessLevel;
import com.plummy.vio.config.Endpoints;
import com.plummy.vio.conversation.Conversation;
import com.plummy.vio.message.*;
import com.plummy.vio.message.MessageContentSerializer;
import com.plummy.vio.tool.Tool;
import com.plummy.vio.tool.ToolRegistry;
import com.plummy.vio.tool.executor.ExecutionRequest;
import com.plummy.vio.tool.executor.ExecutionResult;
import com.plummy.vio.tool.value.argument.Argument;
import com.plummy.vio.tool.value.argument.ArgumentSchemaJsonSerializer;
import com.plummy.vio.tool.value.argument.Arguments;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

public class OpenAIProvider implements Provider {

    private static final OpenAIMessageJsonSerializer OPENAI_MESSAGE_JSON_SERIALIZER = new OpenAIMessageJsonSerializer();

    private final String model;
    private final String apiKey;
    private final HttpClient httpClient;

    public OpenAIProvider(String model, String apiKey) {
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
    public Message request(AccessLevel accessLevel, String instructions, Conversation<?> conversation) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(Endpoints.OPENAI_URI))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer %s".formatted(getApiKey()))
                .POST(HttpRequest.BodyPublishers.ofString(buildRequestBody(accessLevel, instructions, conversation).toString()))
                .build();

        HttpResponse<String> response;

        try {
            response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new ProviderException("Failed to send the OpenAI request", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProviderException("OpenAI request interrupted", e);
        }

        if (response.statusCode() != 200) {
            throw new ProviderException("OpenAI returned %s: %s".formatted(response.statusCode(), response.body()));
        }

        try {
            return parseResponseBody(JsonParser.parseString(response.body()).getAsJsonObject());
        } catch (JsonParseException | IllegalStateException | NullPointerException | Argument.ParseException e) {
            throw new ProviderException("Malformed OpenAI response: %s".formatted(response.body()), e);
        }
    }

    private JsonObject buildRequestBody(AccessLevel accessLevel, String instructions, Conversation<?> conversation) {
        JsonObject json = new JsonObject();

        JsonArray input = new JsonArray();
        for (Message message : conversation.getMessages()) {
            message.accept(OPENAI_MESSAGE_JSON_SERIALIZER).forEach(input::add);
        }

        JsonArray toolDefinitions = new JsonArray();

        for (Tool tool : ToolRegistry.getAll(accessLevel)) {
            toolDefinitions.add(buildToolDefinition(tool));
        }

        JsonObject reasoning = new JsonObject();
        reasoning.addProperty("effort", "medium");

        json.addProperty("model", getModel());
        json.addProperty("instructions", instructions);
        json.add("input", input);
        json.add("tools", toolDefinitions);

        json.addProperty("store", false);
        json.add("reasoning", reasoning);
        // Reasoning counts toward it too.
        json.addProperty("max_output_tokens", 16384);

        return json;
    }

    private static JsonObject buildToolDefinition(Tool tool) {
        JsonObject parameters = ArgumentSchemaJsonSerializer.getInstance().serialize(tool.getArgumentSchema());

        JsonObject json = new JsonObject();

        json.addProperty("type", "function");
        json.addProperty("name", tool.getName());
        json.addProperty("description", tool.getDescription());
        json.add("parameters", parameters);
        json.addProperty("strict", true);

        return json;
    }

    private Message parseResponseBody(JsonObject json) {
        String status = json.get("status").getAsString();

        if (!status.equals("completed")) {
            throw new ProviderException("OpenAI response %s: %s".formatted(status, json.get("incomplete_details")));
        }

        StringBuilder text = new StringBuilder();
        SequencedMap<String, ExecutionRequest> toolExecutionRequests = new LinkedHashMap<>();

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

                        new ExecutionRequest(
                                item.get("name").getAsString(),
                                Arguments.parse(item.get("arguments").getAsString())
                        )
                );
            }
        }

        if (toolExecutionRequests.isEmpty()) {
            return new AgentResponseMessage(text.toString());
        }

        // Text before tool calls is the model thinking out loud; the player sees the calls' status argument instead.
        return new ToolRequestMessage(toolExecutionRequests);
    }

    private static class OpenAIMessageJsonSerializer implements MessageVisitor<List<JsonObject>> {

        @Override
        public List<JsonObject> visitPlayerRequestMessage(PlayerRequestMessage message) {
            JsonObject json = new JsonObject();

            json.addProperty("type", "message");
            json.addProperty("role", "user");
            json.addProperty("content", message.accept(MessageContentSerializer.getInstance()));

            return List.of(json);
        }

        @Override
        public List<JsonObject> visitToolRequestMessage(ToolRequestMessage message) {
            // The status is already inside each call's arguments, so no assistant message is replayed.
            List<JsonObject> items = new ArrayList<>();

            for (Map.Entry<String, ExecutionRequest> entry : message.getToolExecutionRequests().entrySet()) {
                JsonObject json = new JsonObject();

                json.addProperty("type", "function_call");
                json.addProperty("call_id", entry.getKey());
                json.addProperty("name", entry.getValue().getName());
                json.addProperty("arguments", entry.getValue().getArguments().getArguments().toString());

                items.add(json);
            }

            return items;
        }

        @Override
        public List<JsonObject> visitToolResponseMessage(ToolResponseMessage message) {
            List<JsonObject> items = new ArrayList<>();

            for (Map.Entry<String, ExecutionResult> entry : message.getToolExecutionResults().entrySet()) {
                JsonObject output = new JsonObject();

                output.addProperty("status", entry.getValue().getStatus().getValue());
                output.addProperty("result", entry.getValue().getResult());

                JsonObject json = new JsonObject();

                json.addProperty("type", "function_call_output");
                json.addProperty("call_id", entry.getKey());
                json.addProperty("output", output.toString());

                items.add(json);
            }

            return items;
        }

        @Override
        public List<JsonObject> visitAgentResponseMessage(AgentResponseMessage message) {
            JsonObject json = new JsonObject();

            json.addProperty("type", "message");
            json.addProperty("role", "assistant");
            json.addProperty("content", message.getResponse());

            return List.of(json);
        }
    }
}
