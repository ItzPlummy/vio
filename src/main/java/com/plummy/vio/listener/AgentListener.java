package com.plummy.vio.listener;

import com.plummy.vio.message.AgentResponseMessage;
import com.plummy.vio.message.ToolRequestMessage;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public interface AgentListener {

    void onActivity(ServerPlayer player, ToolRequestMessage request);

    void onResponse(ServerPlayer player, AgentResponseMessage response);

    void onError(ServerPlayer player, Component message);
}
