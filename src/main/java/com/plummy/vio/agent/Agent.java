package com.plummy.vio.agent;

import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public interface Agent {

    SubmitResult submit(ServerPlayer player, String request);

    boolean isBusy(UUID playerId);

    boolean reset(UUID playerId);

    void shutdown();
}
