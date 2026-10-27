package com.plummy.vio.message;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.plummy.vio.tool.executor.ExecutionRequest;
import com.plummy.vio.tool.executor.ExecutionResult;

import java.util.List;
import java.util.Map;

public class MessageJsonSerializer implements MessageVisitor<JsonObject> {

    private static final MessageJsonSerializer INSTANCE = new MessageJsonSerializer();

    public static MessageJsonSerializer getInstance() {
        return INSTANCE;
    }

    private MessageJsonSerializer() {
    }

    @Override
    public JsonObject visitPlayerRequestMessage(PlayerRequestMessage message) {
        JsonObject json = new JsonObject();

        json.addProperty("type", MessageType.PLAYER_REQUEST.getValue());
        json.addProperty("dispatch_time", message.getDispatchTime().toString());
        json.addProperty("request", message.getRequest());
        json.add("player_info", serializePlayerInfo(message.getPlayerInfo()));
        json.add("player_location", serializePlayerLocation(message.getPlayerLocation()));
        json.add("player_state", serializePlayerState(message.getPlayerState()));
        json.add("player_inventory", serializePlayerInventory(message.getPlayerInventory()));
        json.add("server_info", serializeServerInfo(message.getServerInfo()));
        json.add("script_info", message.getScriptInfo() == null ? JsonNull.INSTANCE : serializeScriptInfo(message.getScriptInfo()));
        json.addProperty("is_voice", message.isVoice());

        return json;
    }

    @Override
    public JsonObject visitToolRequestMessage(ToolRequestMessage message) {
        JsonObject json = new JsonObject();

        JsonObject toolExecutionRequests = new JsonObject();

        for (Map.Entry<String, ExecutionRequest> entry : message.getToolExecutionRequests().entrySet()) {
            JsonObject toolExecutionRequest = new JsonObject();

            toolExecutionRequest.addProperty("name", entry.getValue().getName());
            toolExecutionRequest.add("arguments", entry.getValue().getArguments().getArguments());

            toolExecutionRequests.add(entry.getKey(), toolExecutionRequest);
        }

        json.addProperty("type", MessageType.TOOL_REQUEST.getValue());
        json.addProperty("dispatch_time", message.getDispatchTime().toString());
        json.addProperty("activity_description", message.getActivityDescription());
        json.add("tool_execution_requests", toolExecutionRequests);

        return json;
    }

    @Override
    public JsonObject visitToolResponseMessage(ToolResponseMessage message) {
        JsonObject json = new JsonObject();

        JsonObject toolExecutionResults = new JsonObject();

        for (Map.Entry<String, ExecutionResult> entry : message.getToolExecutionResults().entrySet()) {
            JsonObject toolExecutionResult = new JsonObject();

            toolExecutionResult.addProperty("status", entry.getValue().getStatus().getValue());
            toolExecutionResult.addProperty("result", entry.getValue().getResult());

            toolExecutionResults.add(entry.getKey(), toolExecutionResult);
        }

        json.addProperty("type", MessageType.TOOL_RESPONSE.getValue());
        json.addProperty("dispatch_time", message.getDispatchTime().toString());
        json.add("tool_execution_results", toolExecutionResults);

        return json;
    }

    @Override
    public JsonObject visitAgentResponseMessage(AgentResponseMessage message) {
        JsonObject json = new JsonObject();

        json.addProperty("type", MessageType.AGENT_RESPONSE.getValue());
        json.addProperty("dispatch_time", message.getDispatchTime().toString());
        json.addProperty("response", message.getResponse());

        return json;
    }

    private static JsonObject serializePlayerInfo(PlayerRequestMessage.PlayerInfo playerInfo) {
        JsonObject json = new JsonObject();

        json.addProperty("id", playerInfo.id().toString());
        json.addProperty("name", playerInfo.name());
        json.addProperty("access_level", playerInfo.accessLevel().getValue());

        return json;
    }

    private static JsonObject serializePlayerLocation(PlayerRequestMessage.PlayerLocation playerLocation) {
        JsonObject json = new JsonObject();

        json.addProperty("x", playerLocation.x());
        json.addProperty("y", playerLocation.y());
        json.addProperty("z", playerLocation.z());
        json.addProperty("yaw", playerLocation.yaw());
        json.addProperty("pitch", playerLocation.pitch());
        json.addProperty("dimension", playerLocation.dimension());
        json.addProperty("biome", playerLocation.biome());
        json.add("structures", serializeStrings(playerLocation.structures()));

        return json;
    }

    private static JsonObject serializePlayerState(PlayerRequestMessage.PlayerState playerState) {
        JsonObject json = new JsonObject();

        json.addProperty("health", playerState.health());
        json.addProperty("armor", playerState.armor());
        json.addProperty("food", playerState.food());
        json.addProperty("saturation", playerState.saturation());
        json.addProperty("level", playerState.level());
        json.addProperty("game_mode", playerState.gameMode());

        return json;
    }

    private static JsonObject serializePlayerInventory(PlayerRequestMessage.PlayerInventory playerInventory) {
        JsonObject json = new JsonObject();

        json.addProperty("main_hand_item", playerInventory.mainHandItem());
        json.addProperty("off_hand_item", playerInventory.offHandItem());

        return json;
    }

    private static JsonObject serializeServerInfo(PlayerRequestMessage.ServerInfo serverInfo) {
        JsonObject json = new JsonObject();

        json.add("dimensions", serializeStrings(serverInfo.dimensions()));
        json.addProperty("time", serverInfo.time());
        json.addProperty("day", serverInfo.day());

        return json;
    }

    private static JsonObject serializeScriptInfo(PlayerRequestMessage.ScriptInfo scriptInfo) {
        JsonArray scripts = new JsonArray();

        for (PlayerRequestMessage.ScriptInfo.Entry entry : scriptInfo.scripts()) {
            JsonObject script = new JsonObject();

            script.addProperty("name", entry.name());
            script.addProperty("type", entry.type().getValue());
            script.addProperty("state", entry.state().getValue());
            script.addProperty("description", entry.description());

            scripts.add(script);
        }

        JsonObject json = new JsonObject();

        json.add("scripts", scripts);

        return json;
    }

    private static JsonArray serializeStrings(List<String> values) {
        JsonArray json = new JsonArray();

        for (String value : values) {
            json.add(value);
        }

        return json;
    }
}
