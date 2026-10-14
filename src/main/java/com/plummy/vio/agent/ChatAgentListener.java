package com.plummy.vio.agent;

import com.plummy.vio.message.ActivityPlaceholder;
import com.plummy.vio.message.AgentResponseMessage;
import com.plummy.vio.section.ActivityDescriptionSection;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

public class ChatAgentListener implements AgentListener {

    @Override
    public void onActivity(ServerPlayer player, ActivityDescriptionSection activity) {
        String text = activity.getActivityDescription().isBlank()
                ? ActivityPlaceholder.random()
                : activity.getActivityDescription();

        player.sendSystemMessage(prefix().append(Component.literal(text).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)));
    }

    @Override
    public void onResponse(ServerPlayer player, AgentResponseMessage response) {
        player.sendSystemMessage(prefix().append(Component.literal(response.getContent())));
    }

    @Override
    public void onError(ServerPlayer player, String message) {
        player.sendSystemMessage(prefix().append(Component.literal(message).withStyle(ChatFormatting.RED)));
    }

    private static MutableComponent prefix() {
        return Component.literal("[Vio] ").withStyle(ChatFormatting.GOLD);
    }
}
