package com.plummy.vio.voice;

import com.plummy.vio.listener.AgentListener;
import com.plummy.vio.message.AgentResponseMessage;
import com.plummy.vio.message.ToolRequestMessage;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

// Speaks replies to spoken requests; the text still reaches the player through the network listener.
public class VoiceAgentListener implements AgentListener {

    private final VoiceManager voiceManager;

    public VoiceAgentListener(VoiceManager voiceManager) {
        this.voiceManager = voiceManager;
    }

    @Override
    public void onActivity(ServerPlayer player, ToolRequestMessage request) {
    }

    @Override
    public void onResponse(ServerPlayer player, AgentResponseMessage response) {
        voiceManager.speakReply(player, response.getResponse());
    }

    @Override
    public void onError(ServerPlayer player, Component message) {
        voiceManager.dropReply(player);
    }
}
