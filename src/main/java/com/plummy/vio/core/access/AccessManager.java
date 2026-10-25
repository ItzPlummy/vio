package com.plummy.vio.core.access;

import com.plummy.vio.config.ServerConfig;
import com.plummy.vio.core.storage.Storage;

import java.util.UUID;

public class AccessManager {

    private final Storage<UUID, PlayerAccess> storage;

    public AccessManager(Storage<UUID, PlayerAccess> storage) {
        this.storage = storage;
    }

    public AccessLevel getServerAccessLevel() {
        return AccessLevel.fromValue(ServerConfig.ACCESS_LEVEL.get());
    }

    public AccessLevel getAccessLevel(UUID playerId) {
        PlayerAccess playerAccess = getPlayerAccess(playerId);

        return playerAccess == null ? getServerAccessLevel() : playerAccess.getLevel();
    }

    public PlayerAccess getPlayerAccess(UUID playerId) {
        return storage.get(playerId);
    }

    public void setPlayerAccessLevel(UUID playerId, String playerName, AccessLevel level) {
        storage.save(new PlayerAccess(playerId, playerName, level));
    }

    public void resetPlayerAccessLevel(UUID playerId) {
        storage.delete(playerId);
    }
}
