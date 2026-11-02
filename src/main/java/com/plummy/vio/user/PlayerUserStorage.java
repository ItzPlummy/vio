package com.plummy.vio.user;

import com.google.gson.JsonObject;
import com.plummy.vio.Vio;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;
import java.util.UUID;

public class PlayerUserStorage extends UserStorage<UUID, PlayerUser> {

    private final Path path;

    public PlayerUserStorage(Path path) {
        this.path = path;
    }

    public static PlayerUserStorage fromServer(MinecraftServer server) {
        return new PlayerUserStorage(server.getWorldPath(new LevelResource("serverconfig")).resolve(Vio.MODID).resolve("players.json"));
    }

    @Override
    public Path getPath() {
        return path;
    }

    @Override
    protected String serializeKey(UUID key) {
        return key.toString();
    }

    @Override
    protected UUID deserializeKey(String value) {
        return UUID.fromString(value);
    }

    @Override
    protected PlayerUser deserializeItemFromJson(UUID key, JsonObject json) {
        return UserJsonDeserializer.getInstance().deserializePlayerUser(key, json);
    }
}
