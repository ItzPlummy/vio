package com.plummy.vio.core.access;

import com.plummy.vio.core.storage.Storage;

import java.util.UUID;

public class PlayerAccess implements Storage.Item<UUID> {

    private final UUID playerId;
    private final String name;
    private final AccessLevel accessLevel;

    public PlayerAccess(UUID playerId, String name, AccessLevel accessLevel) {
        this.playerId = playerId;
        this.name = name;
        this.accessLevel = accessLevel;
    }

    @Override
    public UUID getKey() {
        return getPlayerId();
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getName() {
        return name;
    }

    public AccessLevel getAccessLevel() {
        return accessLevel;
    }
}
