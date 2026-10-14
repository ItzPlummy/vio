package com.plummy.vio.agent;

import com.plummy.vio.message.AgentResponseMessage;
import com.plummy.vio.section.ActivityDescriptionSection;
import net.minecraft.server.level.ServerPlayer;

/**
 * Every callback runs on the server thread and only while the player is online.
 */
public interface AgentListener {

    /**
     * Called for every tool round. The description may be blank if the model did not write one.
     */
    void onActivity(ServerPlayer player, ActivityDescriptionSection activity);

    void onResponse(ServerPlayer player, AgentResponseMessage response);

    void onError(ServerPlayer player, String message);
}
