package com.plummy.vio.provider;

import com.google.gson.*;
import com.plummy.vio.Vio;
import com.plummy.vio.access.AccessLevel;
import com.plummy.vio.conversation.Conversation;
import com.plummy.vio.message.*;
import com.plummy.vio.registry.ToolRegistry;
import com.plummy.vio.section.SectionContentSerializer;
import com.plummy.vio.tool.ExecutionRequest;
import com.plummy.vio.tool.ExecutionResult;
import com.plummy.vio.tool.Tool;
import com.plummy.vio.tool.argument.ArgumentSchemaJsonSerializer;
import com.plummy.vio.tool.argument.Arguments;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

public class OllamaProvider implements Provider {

    // Not localhost: Ollama listens on IPv4 only, and the JVM may resolve localhost to ::1 first.
    private static final String OLLAMA_URI = "http://127.0.0.1:11434/api/chat";

    private final String model;
    private final int contextLength;
    private final HttpClient httpClient;

    public OllamaProvider(String model, int contextLength) {
        this.model = model;
        this.contextLength = contextLength;
        this.httpClient = Vio.getHttpClient();
    }

    public String getModel() {
        return model;
    }

    public int getContextLength() {
        return contextLength;
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    @Override
    public Message request(AccessLevel accessLevel, String instructions, Conversation conversation) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OLLAMA_URI))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(buildRequestBody(accessLevel, instructions, conversation).toString()))
                .build();

        HttpResponse<String> response;

        try {
            response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new ProviderException("Ollama request failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProviderException("Ollama request interrupted", e);
        }

        if (response.statusCode() != 200) {
            throw new ProviderException("Ollama returned " + response.statusCode() + ": " + response.body());
        }

        try {
            return parseResponseBody(JsonParser.parseString(response.body()).getAsJsonObject());
        } catch (JsonParseException | IllegalStateException | NullPointerException | ClassCastException e) {
            throw new ProviderException("Malformed Ollama response: " + response.body(), e);
        }
    }

    private JsonObject buildRequestBody(AccessLevel accessLevel, String instructions, Conversation conversation) {
        JsonObject json = new JsonObject();

        JsonObject system = new JsonObject();
        system.addProperty("role", "system");
        system.addProperty("content", instructions);

        JsonArray messages = new JsonArray();
        messages.add(system);

        // Remembers tool names across messages, so one per request.
        OllamaMessageJsonSerializer serializer = new OllamaMessageJsonSerializer();

        for (Message message : conversation.getMessages()) {
            message.accept(serializer).forEach(messages::add);
        }

        JsonArray toolDefinitions = new JsonArray();

        for (Tool tool : ToolRegistry.getAll(accessLevel)) {
            toolDefinitions.add(buildToolDefinition(tool));
        }

        JsonObject options = new JsonObject();
        options.addProperty("num_ctx", getContextLength());
        // Thinking counts toward it too.
        options.addProperty("num_predict", 16384);

        json.addProperty("model", getModel());
        json.add("messages", messages);
        json.add("tools", toolDefinitions);

        json.addProperty("stream", false);
        // Thinking isn't replayed, so each agent round reasons afresh.
        json.addProperty("think", "medium");
        json.add("options", options);

        return json;
    }

    private static JsonObject buildToolDefinition(Tool tool) {
        JsonObject parameters = ArgumentSchemaJsonSerializer.getInstance().serialize(tool.getFullArgumentSchema());

        JsonObject function = new JsonObject();

        function.addProperty("name", tool.getName());
        function.addProperty("description", tool.getDescription());
        function.add("parameters", parameters);

        JsonObject json = new JsonObject();

        json.addProperty("type", "function");
        json.add("function", function);

        return json;
    }

    private Message parseResponseBody(JsonObject json) {
        JsonElement doneReasonElement = json.get("done_reason");

        if (doneReasonElement != null && doneReasonElement.getAsString().equals("length")) {
            throw new ProviderException("Ollama response hit num_predict");
        }

        JsonObject message = json.getAsJsonObject("message");
        JsonElement contentElement = message.get("content");
        String text = contentElement == null ? "" : contentElement.getAsString();

        SequencedMap<String, ExecutionRequest> toolExecutionRequests = new LinkedHashMap<>();
        JsonElement toolCallsElement = message.get("tool_calls");

        if (toolCallsElement != null && !toolCallsElement.isJsonNull()) {
            for (JsonElement element : toolCallsElement.getAsJsonArray()) {
                JsonObject function = element.getAsJsonObject().getAsJsonObject("function");

                // Ollama matches tool results to calls by order and name, so the ID only keys the conversation.
                toolExecutionRequests.put(
                        UUID.randomUUID().toString(),

                        new ExecutionRequest(
                                function.get("name").getAsString(),
                                new Arguments(function.getAsJsonObject("arguments"))
                        )
                );
            }
        }

        if (toolExecutionRequests.isEmpty()) {
            return AgentResponseMessage.of(text);
        }

        // Text before tool calls is the model thinking out loud; the player sees the calls' status argument instead.
        return ToolRequestMessage.of(toolExecutionRequests);
    }

    private static class OllamaMessageJsonSerializer implements MessageVisitor<List<JsonObject>> {

        // Results don't carry their tool's name, but Ollama wants it, so it's looked up from the preceding calls.
        private final Map<String, String> toolNames = new HashMap<>();

        @Override
        public List<JsonObject> visitPlayerRequestMessage(PlayerRequestMessage message) {
            return List.of(buildMessage("user", SectionContentSerializer.getInstance().getMessageContent(message)));
        }

        @Override
        public List<JsonObject> visitToolRequestMessage(ToolRequestMessage message) {
            // The status is already inside each call's arguments, so no content is replayed.
            JsonArray toolCalls = new JsonArray();

            for (Map.Entry<String, ExecutionRequest> entry : message.getToolExecutionRequests().entrySet()) {
                toolNames.put(entry.getKey(), entry.getValue().getName());

                JsonObject function = new JsonObject();

                function.addProperty("name", entry.getValue().getName());
                function.add("arguments", entry.getValue().getArguments().getArguments());

                JsonObject toolCall = new JsonObject();
                toolCall.add("function", function);

                toolCalls.add(toolCall);
            }

            JsonObject json = buildMessage("assistant", "");
            json.add("tool_calls", toolCalls);

            return List.of(json);
        }

        @Override
        public List<JsonObject> visitToolResponseMessage(ToolResponseMessage message) {
            List<JsonObject> items = new ArrayList<>();

            for (Map.Entry<String, ExecutionResult> entry : message.getToolExecutionResults().entrySet()) {
                JsonObject output = new JsonObject();

                output.addProperty("status", entry.getValue().getStatus().getValue());
                output.addProperty("result", entry.getValue().getResult());

                JsonObject json = buildMessage("tool", output.toString());
                json.addProperty("tool_name", toolNames.getOrDefault(entry.getKey(), ""));

                items.add(json);
            }

            return items;
        }

        @Override
        public List<JsonObject> visitAgentResponseMessage(AgentResponseMessage message) {
            return List.of(buildMessage("assistant", SectionContentSerializer.getInstance().getMessageContent(message)));
        }

        private static JsonObject buildMessage(String role, String content) {
            JsonObject json = new JsonObject();

            json.addProperty("role", role);
            json.addProperty("content", content);

            return json;
        }
    }
}
