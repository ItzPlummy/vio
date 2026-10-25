package com.plummy.vio.core.message;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.plummy.vio.core.section.*;
import com.plummy.vio.core.tool.ExecutionRequest;
import com.plummy.vio.core.tool.ExecutionResult;
import com.plummy.vio.core.tool.ExecutionStatus;
import com.plummy.vio.core.tool.argument.Arguments;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SequencedMap;

public class MessageJsonDeserializer {

    private static final MessageJsonDeserializer INSTANCE = new MessageJsonDeserializer();

    public static MessageJsonDeserializer getInstance() {
        return INSTANCE;
    }

    private MessageJsonDeserializer() {
    }

    public Message deserialize(JsonObject json) {
        return switch (MessageType.fromValue(json.get("type").getAsString())) {
            case PLAYER_REQUEST -> new PlayerRequestMessage(
                    SectionJsonDeserializer.getInstance().deserialize(json.getAsJsonObject("request"), PlayerRequestSection.class),
                    SectionJsonDeserializer.getInstance().deserialize(json.getAsJsonObject("player_context"), PlayerContextSection.class),
                    SectionJsonDeserializer.getInstance().deserialize(json.getAsJsonObject("server_context"), ServerContextSection.class),
                    json.get("script_context") == null ? SectionJsonDeserializer.getInstance().deserialize(json.getAsJsonObject("script_context"), ScriptContextSection.class) : null,
                    Instant.parse(json.get("time").getAsString()),
                    json.get("isVoice").getAsBoolean()
            );
            case TOOL_REQUEST -> {
                SequencedMap<String, ExecutionRequest> toolExecutionRequests = new LinkedHashMap<>();

                for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("tool_execution_requests").entrySet()) {
                    JsonObject toolExecutionRequest = entry.getValue().getAsJsonObject();

                    toolExecutionRequests.put(entry.getKey(), new ExecutionRequest(
                            toolExecutionRequest.get("name").getAsString(),
                            new Arguments(toolExecutionRequest.getAsJsonObject("arguments"))
                    ));
                }

                yield new ToolRequestMessage(
                        SectionJsonDeserializer.getInstance().deserialize(json.getAsJsonObject("activity_description"), ActivityDescriptionSection.class),
                        toolExecutionRequests,
                        Instant.parse(json.get("time").getAsString())
                );
            }
            case TOOL_RESPONSE -> {
                SequencedMap<String, ExecutionResult> toolExecutionResults = new LinkedHashMap<>();

                for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("tool_execution_results").entrySet()) {
                    JsonObject toolExecutionResult = entry.getValue().getAsJsonObject();

                    toolExecutionResults.put(entry.getKey(), new ExecutionResult(
                            ExecutionStatus.fromValue(toolExecutionResult.get("status").getAsString()),
                            toolExecutionResult.get("result").getAsString()
                    ));
                }

                yield new ToolResponseMessage(toolExecutionResults, Instant.parse(json.get("time").getAsString()));
            }
            case AGENT_RESPONSE -> new AgentResponseMessage(
                    SectionJsonDeserializer.getInstance().deserialize(json.getAsJsonObject("response"), TextSection.class),
                    Instant.parse(json.get("time").getAsString())
            );
        };
    }

    public <T extends Message> T deserialize(JsonObject json, Class<T> messageClass) {
        Message message = deserialize(json);

        if (!messageClass.isInstance(message)) {
            throw new JsonParseException("Expected %s, got %s".formatted(messageClass.getSimpleName(), message.getClass().getSimpleName()));
        }

        return messageClass.cast(message);
    }
}
