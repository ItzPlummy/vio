package com.plummy.vio.message;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.plummy.vio.section.*;
import com.plummy.vio.tool.ExecutionRequest;
import com.plummy.vio.tool.ExecutionResult;
import com.plummy.vio.tool.ExecutionStatus;
import com.plummy.vio.tool.argument.Arguments;

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
                    // Absent for players who couldn't use scripts, and in conversations stored before scripts existed.
                    json.has("script_context") ? SectionJsonDeserializer.getInstance().deserialize(json.getAsJsonObject("script_context"), ScriptContextSection.class) : null,
                    getTime(json),
                    // Conversations stored before voice existed have no flag.
                    json.has("voice") && json.get("voice").getAsBoolean()
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
                        getTime(json)
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
