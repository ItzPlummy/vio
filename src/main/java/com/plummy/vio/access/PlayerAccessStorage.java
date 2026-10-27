package com.plummy.vio.access;

import com.google.gson.JsonObject;
import com.plummy.vio.Vio;
import com.plummy.vio.storage.JsonFileStorage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;
import java.util.UUID;

public class PlayerAccessStorage extends JsonFileStorage<UUID, PlayerAccess> {

    private final Path file;

    public PlayerAccessStorage(Path file) {
        this.file = file;
    }

    public static PlayerAccessStorage fromServer(MinecraftServer server) {
        return new PlayerAccessStorage(server.getWorldPath(new LevelResource("serverconfig")).resolve(Vio.MODID).resolve("access.json"));
    }

    @Override
    public Path getPath() {
        return file;
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
    protected JsonObject serializeItemToJson(PlayerAccess item) {
        JsonObject json = new JsonObject();

        json.addProperty("name", item.getName());
        json.addProperty("access_level", item.getAccessLevel().getValue());

        return json;
    }

    @Override
    protected PlayerAccess deserializeItemFromJson(UUID key, JsonObject json) {
        return new PlayerAccess(
                key,
                json.get("name").getAsString(),
                AccessLevel.fromValue(json.get("access_level").getAsString())
        );
    }
}
