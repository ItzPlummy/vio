package com.plummy.vio.client.network;

import com.plummy.vio.client.ClientChatHistory;
import com.plummy.vio.network.payload.*;

public class ClientPayloadHandler {

    public static void handleSubmitResult(SubmitResultPayload payload) {
        ClientChatHistory.getInstance().onSubmitResult(payload.getResult(), payload.getTime());
    }

    public static void handleStatus(AgentStatusPayload payload) {
        ClientChatHistory.getInstance().onStatus(payload.getStatus(), payload.getTime());
    }

    public static void handleResponse(AgentResponsePayload payload) {
        ClientChatHistory.getInstance().onResponse(payload.getResponse(), payload.getTime());
    }

    public static void handleError(AgentErrorPayload payload) {
        ClientChatHistory.getInstance().onError(payload.getError().getString());
    }

    public static void handleHistory(HistoryPayload payload) {
        ClientChatHistory.getInstance().onHistory(payload);
    }

    public static void handleClearHistory() {
        ClientChatHistory.getInstance().clear();
    }
}
