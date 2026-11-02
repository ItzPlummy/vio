package com.plummy.vio.message;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.plummy.vio.tool.executor.ExecutionRequest;
import com.plummy.vio.tool.executor.ExecutionResult;
import com.plummy.vio.tool.executor.ExecutionStatus;
import com.plummy.vio.tool.value.argument.Arguments;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
        Instant dispatchTime = Instant.parse(json.get("dispatch_time").getAsString());

        return switch (MessageType.fromValue(json.get("type").getAsString())) {
            case PLAYER_REQUEST -> new PlayerRequestMessage(
                    json.get("request").getAsString(),
                    deserializePlayerLocation(json.getAsJsonObject("player_location")),
                    deserializePlayerState(json.getAsJsonObject("player_state")),
                    deserializePlayerInventory(json.getAsJsonObject("player_inventory")),
                    deserializeServerInfo(json.getAsJsonObject("server_info")),
                    json.get("is_voice").getAsBoolean(),
                    dispatchTime
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

                yield new ToolRequestMessage(json.get("activity_description").getAsString(), toolExecutionRequests, dispatchTime);
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

                yield new ToolResponseMessage(toolExecutionResults, dispatchTime);
            }
            case AGENT_RESPONSE -> new AgentResponseMessage(json.get("response").getAsString(), dispatchTime);
        };
    }

    public <T extends Message> T deserialize(JsonObject json, Class<T> messageClass) {
        Message message = deserialize(json);

        if (!messageClass.isInstance(message)) {
            throw new JsonParseException("Expected %s, got %s".formatted(messageClass.getSimpleName(), message.getClass().getSimpleName()));
        }

        return messageClass.cast(message);
    }

    private static PlayerRequestMessage.PlayerLocation deserializePlayerLocation(JsonObject json) {
        return new PlayerRequestMessage.PlayerLocation(
                json.get("x").getAsDouble(),
                json.get("y").getAsDouble(),
                json.get("z").getAsDouble(),
                json.get("yaw").getAsDouble(),
                json.get("pitch").getAsDouble(),
                json.get("dimension").getAsString(),
                json.get("biome").getAsString(),
                deserializeStrings(json.getAsJsonArray("structures"))
        );
    }

    private static PlayerRequestMessage.PlayerState deserializePlayerState(JsonObject json) {
        return new PlayerRequestMessage.PlayerState(
                json.get("health").getAsDouble(),
                json.get("food").getAsDouble()
        );
    }

    private static PlayerRequestMessage.PlayerInventory deserializePlayerInventory(JsonObject json) {
        return new PlayerRequestMessage.PlayerInventory(
                json.get("main_hand_item").getAsString(),
                json.get("off_hand_item").getAsString()
        );
    }

    private static PlayerRequestMessage.ServerInfo deserializeServerInfo(JsonObject json) {
        return new PlayerRequestMessage.ServerInfo(
                json.get("time").getAsLong(),
                json.get("day").getAsLong()
        );
    }

    private static List<String> deserializeStrings(JsonArray json) {
        List<String> values = new ArrayList<>();

        for (JsonElement value : json) {
            values.add(value.getAsString());
        }

        return values;
    }
}
