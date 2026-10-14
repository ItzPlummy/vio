package com.plummy.vio.agent;

import com.plummy.vio.message.AgentResponseMessage;
import com.plummy.vio.section.ActivityDescriptionSection;
import net.minecraft.server.level.ServerPlayer;

public interface AgentListener {

    void onActivity(ServerPlayer player, ActivityDescriptionSection activity);

    void onResponse(ServerPlayer player, AgentResponseMessage response);

    void onError(ServerPlayer player, String message);
}
