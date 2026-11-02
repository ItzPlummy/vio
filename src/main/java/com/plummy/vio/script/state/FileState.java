package com.plummy.vio.script.state;

import com.google.gson.JsonObject;
import com.plummy.vio.Vio;
import com.plummy.vio.storage.JsonFileStorage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;

public class FileState extends JsonFileStorage<String, StateItem> {

    private final Path path;

    public FileState(Path path) {
        this.path = path;
    }

    public static FileState fromServer(MinecraftServer server) {
        return new FileState(server.getWorldPath(new LevelResource("serverconfig")).resolve(Vio.MODID).resolve("scripts-state.json"));
    }

    @Override
    public Path getPath() {
        return path;
    }

    @Override
    protected String serializeKey(String key) {
        return key;
    }

    @Override
    protected String deserializeKey(String value) {
        return value;
    }

    @Override
    protected JsonObject serializeItemToJson(StateItem item) {
        JsonObject json = new JsonObject();
        json.add("data", item.getData());
        return json;
    }

    @Override
    protected StateItem deserializeItemFromJson(String key, JsonObject json) {
        return new StateItem(key, json.get("data").getAsJsonObject());
    }
}
