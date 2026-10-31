package com.plummy.vio.script;

import com.google.gson.JsonObject;
import com.plummy.vio.Vio;
import com.plummy.vio.storage.JsonFileStorage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;

public class ScriptStorage extends JsonFileStorage<ScriptKey, Script> {

    private final Path path;

    public ScriptStorage(Path path) {
        this.path = path;
    }

    public static ScriptStorage fromServer(MinecraftServer server) {
        return new ScriptStorage(server.getWorldPath(new LevelResource("serverconfig")).resolve(Vio.MODID).resolve("scripts.json"));
    }

    @Override
    public Path getPath() {
        return path;
    }

    @Override
    protected String serializeKey(ScriptKey key) {
        return key.getKey();
    }

    @Override
    protected ScriptKey deserializeKey(String value) {
        return new ScriptKey(value);
    }

    @Override
    protected JsonObject serializeItemToJson(Script item) {
        return item.accept(ScriptJsonSerializer.getInstance());
    }

    @Override
    protected Script deserializeItemFromJson(ScriptKey key, JsonObject json) {
        return ScriptJsonDeserializer.getInstance().deserialize(key, json);
    }
}
