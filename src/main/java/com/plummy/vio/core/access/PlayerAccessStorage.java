package com.plummy.vio.core.access;

import com.google.gson.JsonObject;
import com.plummy.vio.Vio;
import com.plummy.vio.core.storage.JsonFileStorage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;
import java.util.UUID;

public class PlayerAccessStorage extends JsonFileStorage<UUID, PlayerAccess> {

    public PlayerAccessStorage(Path file) {
        super(file);
    }

    public static PlayerAccessStorage fromServer(MinecraftServer server) {
        return new PlayerAccessStorage(server.getWorldPath(new LevelResource("serverconfig")).resolve(Vio.MODID).resolve("access.json"));
    }

    @Override
    protected JsonObject serialize(PlayerAccess value) {
        JsonObject json = new JsonObject();

        json.addProperty("name", value.getName());
        json.addProperty("access_level", value.getAccessLevel().getValue());

        return json;
    }

    @Override
    protected PlayerAccess deserialize(UUID key, JsonObject json) {
        return new PlayerAccess(
                key,
                json.get("name").getAsString(),
                AccessLevel.fromValue(json.get("access_level").getAsString())
        );
    }

    @Override
    protected UUID keyFromString(String key) {
        return UUID.fromString(key);
    }

    @Override
    protected String keyToString(UUID key) {
        return key.toString();
    }
}
