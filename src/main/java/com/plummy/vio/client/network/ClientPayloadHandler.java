package com.plummy.vio.client.network;

import com.plummy.vio.client.conversation.ClientConversation;
import com.plummy.vio.network.payload.*;

public class ClientPayloadHandler {

    public static void handleSubmitResult(SubmitResultPayload payload) {
        ClientConversation.getInstance().onSubmitResult(payload.getResult(), payload.getTime());
    }

    public static void handleStatus(AgentStatusPayload payload) {
        ClientConversation.getInstance().onStatus(payload.getStatus(), payload.getTime());
    }

    public static void handleResponse(AgentResponsePayload payload) {
        ClientConversation.getInstance().onResponse(payload.getResponse(), payload.getTime());
    }

    public static void handleError(AgentErrorPayload payload) {
        ClientConversation.getInstance().onError(payload.getError().getString());
    }

    public static void handleHistory(HistoryPayload payload) {
        ClientConversation.getInstance().onHistory(payload);
    }

    public static void handleClearHistory() {
        ClientConversation.getInstance().clear();
    }
}
