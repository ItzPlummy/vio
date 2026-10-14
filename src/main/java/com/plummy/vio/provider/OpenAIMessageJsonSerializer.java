package com.plummy.vio.provider;

import com.google.gson.JsonObject;
import com.plummy.vio.message.AgentResponseMessage;
import com.plummy.vio.message.MessageVisitor;
import com.plummy.vio.message.PlayerRequestMessage;
import com.plummy.vio.message.SystemMessage;
import com.plummy.vio.message.ToolRequestMessage;
import com.plummy.vio.message.ToolResponseMessage;
import com.plummy.vio.tool.ToolExecutionRequest;
import com.plummy.vio.tool.ToolExecutionResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OpenAIMessageJsonSerializer implements MessageVisitor<List<JsonObject>> {

    @Override
    public List<JsonObject> visitSystemMessage(SystemMessage message) {
        JsonObject json = new JsonObject();

        json.addProperty("type", "message");
        json.addProperty("role", "system");
        json.addProperty("content", message.getContent());

        return List.of(json);
    }

    @Override
    public List<JsonObject> visitPlayerRequestMessage(PlayerRequestMessage message) {
        JsonObject json = new JsonObject();

        json.addProperty("type", "message");
        json.addProperty("role", "user");
        json.addProperty("content", message.getContent());

        return List.of(json);
    }

    @Override
    public List<JsonObject> visitToolRequestMessage(ToolRequestMessage message) {
        List<JsonObject> items = new ArrayList<>();

        String activityDescription = message.getActivityDescription().getText();

        if (!activityDescription.isBlank()) {
            JsonObject json = new JsonObject();

            json.addProperty("type", "message");
            json.addProperty("role", "assistant");
            json.addProperty("content", activityDescription);

            items.add(json);
        }

        for (Map.Entry<String, ToolExecutionRequest> entry : message.getToolExecutionRequests().entrySet()) {
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

        for (Map.Entry<String, ToolExecutionResult> entry : message.getToolExecutionResults().entrySet()) {
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
        json.addProperty("content", message.getContent());

        return List.of(json);
    }
}
