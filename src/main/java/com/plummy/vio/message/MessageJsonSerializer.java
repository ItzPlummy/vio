package com.plummy.vio.message;

import com.google.gson.JsonObject;
import com.plummy.vio.registry.VisitorRegistry;
import com.plummy.vio.tool.ToolExecutionRequest;
import com.plummy.vio.tool.ToolExecutionResult;

import java.util.Map;

public class MessageJsonSerializer implements MessageVisitor<JsonObject> {

    @Override
    public JsonObject visitSystemMessage(SystemMessage message) {
        JsonObject json = new JsonObject();

        json.addProperty("type", message.getType().getValue());
        json.add("text", message.getText().accept(VisitorRegistry.SECTION_JSON_SERIALIZER));

        return json;
    }

    @Override
    public JsonObject visitPlayerRequestMessage(PlayerRequestMessage message) {
        JsonObject json = new JsonObject();

        json.addProperty("type", message.getType().getValue());
        json.add("request", message.getRequest().accept(VisitorRegistry.SECTION_JSON_SERIALIZER));

        return json;
    }

    @Override
    public JsonObject visitToolRequestMessage(ToolRequestMessage message) {
        JsonObject json = new JsonObject();

        JsonObject toolExecutionRequests = new JsonObject();

        for (Map.Entry<String, ToolExecutionRequest> entry : message.getToolExecutionRequests().entrySet()) {
            JsonObject toolExecutionRequest = new JsonObject();

            toolExecutionRequest.addProperty("name", entry.getValue().getName());
            toolExecutionRequest.add("arguments", entry.getValue().getArguments().getArguments());

            toolExecutionRequests.add(entry.getKey(), toolExecutionRequest);
        }

        json.addProperty("type", message.getType().getValue());
        json.add("activity_description", message.getActivityDescription().accept(VisitorRegistry.SECTION_JSON_SERIALIZER));
        json.add("tool_execution_requests", toolExecutionRequests);

        return json;
    }

    @Override
    public JsonObject visitToolResponseMessage(ToolResponseMessage message) {
        JsonObject json = new JsonObject();

        JsonObject toolExecutionResults = new JsonObject();

        for (Map.Entry<String, ToolExecutionResult> entry : message.getToolExecutionResults().entrySet()) {
            JsonObject toolExecutionResult = new JsonObject();

            toolExecutionResult.addProperty("status", entry.getValue().getStatus().getValue());
            toolExecutionResult.addProperty("result", entry.getValue().getResult());

            toolExecutionResults.add(entry.getKey(), toolExecutionResult);
        }

        json.addProperty("type", message.getType().getValue());
        json.add("tool_execution_results", toolExecutionResults);

        return json;
    }

    @Override
    public JsonObject visitAgentResponseMessage(AgentResponseMessage message) {
        JsonObject json = new JsonObject();

        json.addProperty("type", message.getType().getValue());
        json.add("response", message.getResponse().accept(VisitorRegistry.SECTION_JSON_SERIALIZER));

        return json;
    }
}
