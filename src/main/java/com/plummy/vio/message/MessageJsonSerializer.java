package com.plummy.vio.message;

import com.google.gson.JsonObject;
import com.plummy.vio.section.SectionJsonSerializer;
import com.plummy.vio.tool.Tool;

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

        json.addProperty("type", message.getType().getValue());
        json.addProperty("time", message.getTime().toString());
        json.add("player_context", message.getPlayerContext().accept(SectionJsonSerializer.getInstance()));
        json.add("server_context", message.getServerContext().accept(SectionJsonSerializer.getInstance()));
        json.add("request", message.getRequest().accept(SectionJsonSerializer.getInstance()));

        return json;
    }

    @Override
    public JsonObject visitToolRequestMessage(ToolRequestMessage message) {
        JsonObject json = new JsonObject();

        JsonObject toolExecutionRequests = new JsonObject();

        for (Map.Entry<String, Tool.ExecutionRequest> entry : message.getToolExecutionRequests().entrySet()) {
            JsonObject toolExecutionRequest = new JsonObject();

            toolExecutionRequest.addProperty("name", entry.getValue().getName());
            toolExecutionRequest.add("arguments", entry.getValue().getArguments().getArguments());

            toolExecutionRequests.add(entry.getKey(), toolExecutionRequest);
        }

        json.addProperty("type", message.getType().getValue());
        json.addProperty("time", message.getTime().toString());
        json.add("activity_description", message.getActivityDescription().accept(SectionJsonSerializer.getInstance()));
        json.add("tool_execution_requests", toolExecutionRequests);

        return json;
    }

    @Override
    public JsonObject visitToolResponseMessage(ToolResponseMessage message) {
        JsonObject json = new JsonObject();

        JsonObject toolExecutionResults = new JsonObject();

        for (Map.Entry<String, Tool.ExecutionResult> entry : message.getToolExecutionResults().entrySet()) {
            JsonObject toolExecutionResult = new JsonObject();

            toolExecutionResult.addProperty("status", entry.getValue().getStatus().getValue());
            toolExecutionResult.addProperty("result", entry.getValue().getResult());

            toolExecutionResults.add(entry.getKey(), toolExecutionResult);
        }

        json.addProperty("type", message.getType().getValue());
        json.addProperty("time", message.getTime().toString());
        json.add("tool_execution_results", toolExecutionResults);

        return json;
    }

    @Override
    public JsonObject visitAgentResponseMessage(AgentResponseMessage message) {
        JsonObject json = new JsonObject();

        json.addProperty("type", message.getType().getValue());
        json.addProperty("time", message.getTime().toString());
        json.add("response", message.getResponse().accept(SectionJsonSerializer.getInstance()));

        return json;
    }
}
