package com.plummy.vio.core.tool;

import com.plummy.vio.core.access.AccessLevel;

import java.util.UUID;

// The player a tool call runs for.
public class Requester {

    private final UUID playerId;
    private final AccessLevel accessLevel;

    public Requester(UUID playerId, AccessLevel accessLevel) {
        this.playerId = playerId;
        this.accessLevel = accessLevel;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public AccessLevel getAccessLevel() {
        return accessLevel;
    }
}
