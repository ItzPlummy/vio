package com.plummy.vio.provider;

import com.google.gson.*;
import com.plummy.vio.Vio;
import com.plummy.vio.access.AccessLevel;
import com.plummy.vio.config.Endpoints;
import com.plummy.vio.conversation.Conversation;
import com.plummy.vio.message.*;
import com.plummy.vio.section.SectionContentSerializer;
import com.plummy.vio.tool.Tool;
import com.plummy.vio.tool.ToolRegistry;
import com.plummy.vio.tool.executor.ExecutionRequest;
import com.plummy.vio.tool.executor.ExecutionResult;
import com.plummy.vio.tool.executor.ExecutionStatus;
import com.plummy.vio.tool.value.argument.ArgumentSchemaJsonSerializer;
import com.plummy.vio.tool.value.argument.Arguments;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;

public class AnthropicProvider implements Provider {

    private static final String REFUSAL_RESPONSE = "Sorry, I can't help with that one.";

    private static final AnthropicMessageJsonSerializer ANTHROPIC_MESSAGE_JSON_SERIALIZER = new AnthropicMessageJsonSerializer();

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
    public Message request(AccessLevel accessLevel, String instructions, Conversation conversation) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(Endpoints.ANTHROPIC_URI))
                .header("Content-Type", "application/json")
                .header("x-api-key", getApiKey())
                .header("anthropic-version", Endpoints.ANTHROPIC_VERSION)
                .POST(HttpRequest.BodyPublishers.ofString(buildRequestBody(accessLevel, instructions, conversation).toString()))
                .build();

        HttpResponse<String> response;

        try {
            response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new ProviderException("Failed to send the Anthropic request", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProviderException("Anthropic request interrupted", e);
        }

        if (response.statusCode() != 200) {
            throw new ProviderException("Anthropic returned %s: %s".formatted(response.statusCode(), response.body()));
        }

        try {
            return parseResponseBody(JsonParser.parseString(response.body()).getAsJsonObject());
        } catch (JsonParseException | IllegalStateException | NullPointerException | ClassCastException e) {
            throw new ProviderException("Malformed Anthropic response: %s".formatted(response.body()), e);
        }
    }

    private JsonObject buildRequestBody(AccessLevel accessLevel, String instructions, Conversation conversation) {
        JsonObject json = new JsonObject();

        JsonArray messages = new JsonArray();
        for (Message message : conversation.getMessages()) {
            message.accept(ANTHROPIC_MESSAGE_JSON_SERIALIZER).forEach(messages::add);
        }

        JsonArray toolDefinitions = new JsonArray();

        for (Tool tool : ToolRegistry.getAll(accessLevel)) {
            toolDefinitions.add(buildToolDefinition(tool));
        }

        // No thinking field: current models then think adaptively, and effort sets how much. Thinking blocks aren't
        // stored or replayed, so each agent round reasons afresh instead of seeing the previous round's reasoning.
        JsonObject outputConfig = new JsonObject();
        outputConfig.addProperty("effort", "medium");

        // Every agent round resends the whole conversation, so cache it up to the last block.
        JsonObject cacheControl = new JsonObject();
        cacheControl.addProperty("type", "ephemeral");

        json.addProperty("model", getModel());
        json.addProperty("system", instructions);
        json.add("messages", messages);

        if (!toolDefinitions.isEmpty()) {
            json.add("tools", toolDefinitions);
        }

        json.add("output_config", outputConfig);
        json.add("cache_control", cacheControl);
        // Thinking counts toward it too.
        json.addProperty("max_tokens", 16384);

        return json;
    }

    private static JsonObject buildToolDefinition(Tool tool) {
        JsonObject inputSchema = ArgumentSchemaJsonSerializer.getInstance().serialize(tool.getArgumentSchema());

        JsonObject json = new JsonObject();

        json.addProperty("name", tool.getName());
        json.addProperty("description", tool.getDescription());
        json.add("input_schema", inputSchema);

        return json;
    }

    private Message parseResponseBody(JsonObject json) {
        JsonElement stopReasonElement = json.get("stop_reason");
        String stopReason = stopReasonElement == null || stopReasonElement.isJsonNull() ? "" : stopReasonElement.getAsString();

        switch (stopReason) {
            case "refusal" -> {
                Vio.getLogger().warn("Anthropic declined the request: %s".formatted(json.get("stop_details")));
                return AgentResponseMessage.of(REFUSAL_RESPONSE);
            }
            case "max_tokens" -> throw new ProviderException("Anthropic response hit max_tokens");
        }

        StringBuilder text = new StringBuilder();
        SequencedMap<String, ExecutionRequest> toolExecutionRequests = new LinkedHashMap<>();

        for (JsonElement element : json.getAsJsonArray("content")) {
            JsonObject block = element.getAsJsonObject();

            switch (block.get("type").getAsString()) {
                case "text" -> text.append(block.get("text").getAsString());
                case "tool_use" -> toolExecutionRequests.put(
                        block.get("id").getAsString(),

                        new ExecutionRequest(
                                block.get("name").getAsString(),
                                new Arguments(block.getAsJsonObject("input"))
                        )
                );
            }
        }

        if (toolExecutionRequests.isEmpty()) {
            return AgentResponseMessage.of(text.toString());
        }

        // Text before tool calls is the model thinking out loud; the player sees the calls' status argument instead.
        return ToolRequestMessage.of(toolExecutionRequests);
    }

    private static class AnthropicMessageJsonSerializer implements MessageVisitor<List<JsonObject>> {

        @Override
        public List<JsonObject> visitPlayerRequestMessage(PlayerRequestMessage message) {
            return List.of(buildMessage("user", SectionContentSerializer.getInstance().getMessageContent(message)));
        }

        @Override
        public List<JsonObject> visitToolRequestMessage(ToolRequestMessage message) {
            // The status is already inside each call's arguments, so no text block is replayed.
            JsonArray content = new JsonArray();

            for (Map.Entry<String, ExecutionRequest> entry : message.getToolExecutionRequests().entrySet()) {
                JsonObject block = new JsonObject();

                block.addProperty("type", "tool_use");
                block.addProperty("id", entry.getKey());
                block.addProperty("name", entry.getValue().getName());
                block.add("input", entry.getValue().getArguments().getArguments());

                content.add(block);
            }

            return List.of(buildMessage("assistant", content));
        }

        @Override
        public List<JsonObject> visitToolResponseMessage(ToolResponseMessage message) {
            JsonArray content = new JsonArray();

            for (Map.Entry<String, ExecutionResult> entry : message.getToolExecutionResults().entrySet()) {
                JsonObject output = new JsonObject();

                output.addProperty("status", entry.getValue().getStatus().getValue());
                output.addProperty("result", entry.getValue().getResult());

                JsonObject block = new JsonObject();

                block.addProperty("type", "tool_result");
                block.addProperty("tool_use_id", entry.getKey());
                block.addProperty("content", output.toString());

                ExecutionStatus status = entry.getValue().getStatus();

                if (status != ExecutionStatus.SUCCESS) {
                    block.addProperty("is_error", true);
                }

                content.add(block);
            }

            return List.of(buildMessage("user", content));
        }

        @Override
        public List<JsonObject> visitAgentResponseMessage(AgentResponseMessage message) {
            String text = SectionContentSerializer.getInstance().getMessageContent(message);

            // The API rejects empty assistant content; dropping the turn is fine, since consecutive user turns are merged.
            if (text.isBlank()) {
                return List.of();
            }

            return List.of(buildMessage("assistant", text));
        }

        private static JsonObject buildMessage(String role, String text) {
            JsonObject json = new JsonObject();

            json.addProperty("role", role);
            json.addProperty("content", text);

            return json;
        }

        private static JsonObject buildMessage(String role, JsonArray content) {
            JsonObject json = new JsonObject();

            json.addProperty("role", role);
            json.add("content", content);

            return json;
        }
    }
}
