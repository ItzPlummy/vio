package com.plummy.vio.agent;

import net.minecraft.server.level.ServerPlayer;

import java.time.Instant;
import java.util.UUID;

public interface Agent {

    // `time` is when the request arrived; it becomes the stored request's time. `voice` marks a transcribed request.
    SubmitResult submit(ServerPlayer player, String request, Instant time, boolean voice);

    boolean isBusy(UUID playerId);

    boolean reset(UUID playerId);

    void shutdown();
}
