package com.plummy.vio.script;

import com.plummy.vio.Vio;
import com.plummy.vio.storage.GroovyDirectoryStorage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;

public class ScriptSourceStorage extends GroovyDirectoryStorage<ScriptKey, ScriptSource> {

    private final Path path;

    public ScriptSourceStorage(Path path) {
        this.path = path;
    }

    public static ScriptSourceStorage fromServer(MinecraftServer server) {
        return new ScriptSourceStorage(server.getWorldPath(new LevelResource("serverconfig")).resolve(Vio.MODID).resolve("scripts"));
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
    protected String serializeItem(ScriptSource item) {
        return item.getSource();
    }

    @Override
    protected ScriptSource deserializeItem(ScriptKey key, String value) {
        return new ScriptSource(key, value);
    }
}
