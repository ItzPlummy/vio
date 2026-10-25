package com.plummy.vio.core.tool.requester;

import com.plummy.vio.core.access.AccessLevel;

import java.util.UUID;

public class PlayerToolRequester implements ToolRequester {

    private final UUID playerId;
    private final AccessLevel accessLevel;

    public PlayerToolRequester(UUID playerId, AccessLevel accessLevel) {
        this.playerId = playerId;
        this.accessLevel = accessLevel;
    }

    @Override
    public AccessLevel getAccessLevel() {
        return accessLevel;
    }

    public UUID getPlayerId() {
        return playerId;
    }
}
