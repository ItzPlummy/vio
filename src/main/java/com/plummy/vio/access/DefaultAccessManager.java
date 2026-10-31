package com.plummy.vio.access;

import com.mojang.authlib.GameProfile;
import com.plummy.vio.config.ServerConfig;
import com.plummy.vio.storage.Storage;
import com.plummy.vio.user.PlayerUser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.GameProfileCache;

import java.util.UUID;

public class DefaultAccessManager implements AccessManager {

    private final MinecraftServer server;
    private final Storage<UUID, PlayerUser> playerUserStorage;

    public DefaultAccessManager(MinecraftServer server, Storage<UUID, PlayerUser> playerUserStorage) {
        this.server = server;
        this.playerUserStorage = playerUserStorage;
    }

    @Override
    public AccessLevel getDefaultAccessLevel() {
        return AccessLevel.fromValue(ServerConfig.DEFAULT_ACCESS_LEVEL.get());
    }

    @Override
    public AccessLevel getPlayerAccessLevel(UUID playerId) {
        PlayerUser playerUser = getPlayerUser(playerId);
        return playerUser == null ? getDefaultAccessLevel() : playerUser.getAccessLevel();
    }

    @Override
    public PlayerUser getPlayerUser(UUID playerId) {
        return playerUserStorage.get(playerId);
    }

    @Override
    public void setPlayerAccessLevel(UUID playerId, AccessLevel accessLevel) {
        playerUserStorage.save(new PlayerUser(playerId, getPlayerName(playerId), accessLevel));
    }

    @Override
    public void resetPlayerAccessLevel(UUID playerId) {
        playerUserStorage.delete(playerId);
    }

    // A stored player keeps their name; a new one gets it from the profile cache, or the id when it's unknown there
    private String getPlayerName(UUID playerId) {
        PlayerUser playerUser = getPlayerUser(playerId);

        if (playerUser != null) {
            return playerUser.getName();
        }

        GameProfileCache cache = server.getProfileCache();

        return cache == null ? playerId.toString() : cache.get(playerId).map(GameProfile::getName).orElse(playerId.toString());
    }
}
