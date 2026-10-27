package com.plummy.vio.access;

import com.plummy.vio.storage.StorageItem;

import java.util.UUID;

public class PlayerAccess implements StorageItem<UUID> {

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
