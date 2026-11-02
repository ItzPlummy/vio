package com.plummy.vio.listener;

import com.plummy.vio.message.AgentResponseMessage;
import com.plummy.vio.message.ToolRequestMessage;
import com.plummy.vio.network.Networking;
import com.plummy.vio.network.payload.AgentErrorPayload;
import com.plummy.vio.network.payload.AgentResponsePayload;
import com.plummy.vio.network.payload.AgentStatusPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class NetworkAgentListener implements AgentListener {

    @Override
    public void onActivity(ServerPlayer player, ToolRequestMessage request) {
        Networking.send(player, new AgentStatusPayload(request.getActivityDescription(), request.getDispatchTime()));
    }

    @Override
    public void onResponse(ServerPlayer player, AgentResponseMessage response) {
        Networking.send(player, new AgentResponsePayload(response.getResponse(), response.getDispatchTime()));
    }

    @Override
    public void onError(ServerPlayer player, Component message) {
        Networking.send(player, new AgentErrorPayload(message));
    }
}
