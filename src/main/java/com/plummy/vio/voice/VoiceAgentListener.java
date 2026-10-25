package com.plummy.vio.voice;

import com.plummy.vio.core.message.AgentResponseMessage;
import com.plummy.vio.core.message.ToolRequestMessage;
import com.plummy.vio.core.section.SectionContentSerializer;
import com.plummy.vio.listener.AgentListener;
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
        voiceManager.speakReply(player, SectionContentSerializer.getInstance().getMessageContent(response));
    }

    @Override
    public void onError(ServerPlayer player, Component message) {
        voiceManager.dropReply(player);
    }
}
