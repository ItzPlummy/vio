package com.plummy.vio.message;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.plummy.vio.registry.DeserializerRegistry;
import com.plummy.vio.section.ActivityDescriptionSection;
import com.plummy.vio.section.PlayerContextSection;
import com.plummy.vio.section.PlayerRequestSection;
import com.plummy.vio.section.ServerContextSection;
import com.plummy.vio.section.TextSection;
import com.plummy.vio.tool.ToolArguments;
import com.plummy.vio.tool.ToolExecutionRequest;
import com.plummy.vio.tool.ToolExecutionResult;
import com.plummy.vio.tool.ToolExecutionStatus;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SequencedMap;

public class MessageJsonDeserializer {

    public Message deserialize(JsonObject json) {
        return switch (MessageType.fromValue(json.get("type").getAsString())) {
            case SYSTEM -> new SystemMessage(
                    DeserializerRegistry.SECTION_JSON_DESERIALIZER.deserialize(json.getAsJsonObject("text"), TextSection.class)
            );
            case PLAYER_REQUEST -> new PlayerRequestMessage(
                    DeserializerRegistry.SECTION_JSON_DESERIALIZER.deserialize(json.getAsJsonObject("request"), PlayerRequestSection.class),
                    DeserializerRegistry.SECTION_JSON_DESERIALIZER.deserialize(json.getAsJsonObject("player_context"), PlayerContextSection.class),
                    DeserializerRegistry.SECTION_JSON_DESERIALIZER.deserialize(json.getAsJsonObject("server_context"), ServerContextSection.class)
            );
            case TOOL_REQUEST -> {
                SequencedMap<String, ToolExecutionRequest> toolExecutionRequests = new LinkedHashMap<>();

                for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("tool_execution_requests").entrySet()) {
                    JsonObject toolExecutionRequest = entry.getValue().getAsJsonObject();

                    toolExecutionRequests.put(entry.getKey(), new ToolExecutionRequest(
                            toolExecutionRequest.get("name").getAsString(),
                            new ToolArguments(toolExecutionRequest.getAsJsonObject("arguments"))
                    ));
                }

                yield new ToolRequestMessage(
                        DeserializerRegistry.SECTION_JSON_DESERIALIZER.deserialize(json.getAsJsonObject("activity_description"), ActivityDescriptionSection.class),
                        toolExecutionRequests
                );
            }
            case TOOL_RESPONSE -> {
                SequencedMap<String, ToolExecutionResult> toolExecutionResults = new LinkedHashMap<>();

                for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("tool_execution_results").entrySet()) {
                    JsonObject toolExecutionResult = entry.getValue().getAsJsonObject();

                    toolExecutionResults.put(entry.getKey(), new ToolExecutionResult(
                            ToolExecutionStatus.fromValue(toolExecutionResult.get("status").getAsString()),
                            toolExecutionResult.get("result").getAsString()
                    ));
                }

                yield new ToolResponseMessage(toolExecutionResults);
            }
            case AGENT_RESPONSE -> new AgentResponseMessage(
                    DeserializerRegistry.SECTION_JSON_DESERIALIZER.deserialize(json.getAsJsonObject("response"), TextSection.class)
            );
        };
    }

    public <T extends Message> T deserialize(JsonObject json, Class<T> expectedClass) {
        Message message = deserialize(json);

        if (!expectedClass.isInstance(message)) {
            throw new JsonParseException("Expected " + expectedClass.getSimpleName() + ", got " + message.getType());
        }

        return expectedClass.cast(message);
    }
}
