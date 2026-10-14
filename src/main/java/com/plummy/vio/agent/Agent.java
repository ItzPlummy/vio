package com.plummy.vio.agent;

import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public interface Agent {

    /**
     * Server thread. Captures the player's context and starts processing on a worker; never blocks.
     * Results are delivered through the {@link AgentListener}.
     */
    SubmitResult submit(ServerPlayer player, String request);

    boolean isBusy(UUID playerId);

    /**
     * Deletes the player's conversation. Refused while the player has a request in progress.
     */
    boolean reset(UUID playerId);

    /**
     * Stops accepting requests and interrupts running ones.
     */
    void shutdown();
}
