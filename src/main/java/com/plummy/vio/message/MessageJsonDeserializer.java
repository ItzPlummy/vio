package com.plummy.vio.message;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.plummy.vio.section.*;
import com.plummy.vio.tool.Tool;

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
                    getTime(json)
            );
            case TOOL_REQUEST -> {
                SequencedMap<String, Tool.ExecutionRequest> toolExecutionRequests = new LinkedHashMap<>();

                for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("tool_execution_requests").entrySet()) {
                    JsonObject toolExecutionRequest = entry.getValue().getAsJsonObject();

                    toolExecutionRequests.put(entry.getKey(), new Tool.ExecutionRequest(
                            toolExecutionRequest.get("name").getAsString(),
                            new Tool.Arguments(toolExecutionRequest.getAsJsonObject("arguments"))
                    ));
                }

                yield new ToolRequestMessage(
                        SectionJsonDeserializer.getInstance().deserialize(json.getAsJsonObject("activity_description"), ActivityDescriptionSection.class),
                        toolExecutionRequests,
                        getTime(json)
                );
            }
            case TOOL_RESPONSE -> {
                SequencedMap<String, Tool.ExecutionResult> toolExecutionResults = new LinkedHashMap<>();

                for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("tool_execution_results").entrySet()) {
                    JsonObject toolExecutionResult = entry.getValue().getAsJsonObject();

                    toolExecutionResults.put(entry.getKey(), new Tool.ExecutionResult(
                            Tool.ExecutionStatus.fromValue(toolExecutionResult.get("status").getAsString()),
                            toolExecutionResult.get("result").getAsString()
                    ));
                }

                yield new ToolResponseMessage(toolExecutionResults, getTime(json));
            }
            case AGENT_RESPONSE -> new AgentResponseMessage(
                    SectionJsonDeserializer.getInstance().deserialize(json.getAsJsonObject("response"), TextSection.class),
                    getTime(json)
            );
        };
    }

    // Stored as ISO-8601 in UTC, e.g. 2026-10-16T14:05:09.123Z.
    private static Instant getTime(JsonObject json) {
        return Instant.parse(json.get("time").getAsString());
    }

    public <T extends Message> T deserialize(JsonObject json, Class<T> expectedClass) {
        Message message = deserialize(json);

        if (!expectedClass.isInstance(message)) {
            throw new JsonParseException("Expected " + expectedClass.getSimpleName() + ", got " + message.getType());
        }

        return expectedClass.cast(message);
    }
}
