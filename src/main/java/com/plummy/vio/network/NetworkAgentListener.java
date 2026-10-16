package com.plummy.vio.network;

import com.plummy.vio.agent.AgentListener;
import com.plummy.vio.message.AgentResponseMessage;
import com.plummy.vio.message.ToolRequestMessage;
import com.plummy.vio.network.payload.AgentErrorPayload;
import com.plummy.vio.network.payload.AgentResponsePayload;
import com.plummy.vio.network.payload.AgentStatusPayload;
import com.plummy.vio.section.SectionContentSerializer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

// Sends agent output to the Vio client UI. Only players with the mod can send requests, so there is no chat fallback.
public class NetworkAgentListener implements AgentListener {

    @Override
    public void onActivity(ServerPlayer player, ToolRequestMessage request) {
        // A blank status is sent as is: the client picks a placeholder in the player's language.
        Networking.send(player, new AgentStatusPayload(request.getActivityDescription().getActivityDescription(), request.getTime()));
    }

    @Override
    public void onResponse(ServerPlayer player, AgentResponseMessage response) {
        Networking.send(player, new AgentResponsePayload(SectionContentSerializer.getInstance().getMessageContent(response), response.getTime()));
    }

    @Override
    public void onError(ServerPlayer player, Component message) {
        Networking.send(player, new AgentErrorPayload(message));
    }
}
