package com.plummy.vio.script;

import com.plummy.vio.Vio;
import com.plummy.vio.storage.GroovyDirectoryStorage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

public class ScriptStorage extends GroovyDirectoryStorage<String, Script> {

    public static final String NAME_RULE = "1 to 48 characters: lowercase letters, digits and underscores, starting with a letter";

    private static final String EXTENSION = ".groovy";
    private static final Pattern NAME =Pattern.compile("[a-z][a-z0-9_]{0,47}");
    private static final Pattern RESERVED_NAME = Pattern.compile("con|prn|aux|nul|com[0-9]|lpt[0-9]");

    public ScriptStorage(Path directory) {
        super(directory);
    }

    public static ScriptStorage fromServer(MinecraftServer server) {
        return new ScriptStorage(server.getWorldPath(new LevelResource("serverconfig")).resolve(Vio.MODID).resolve("scripts"));
    }

    @Override
    protected String serialize(Script value) {
        return value.accept(ScriptSerializer.getInstance());
    }

    @Override
    protected Script deserialize(String key, String source) {
        return ScriptDeserializer.getInstance().deserialize(key, source);
    }

    @Override
    protected String getFileName(String key) {
        if (!isValidName(key)) {
            throw new IllegalArgumentException("Invalid script name: %s".formatted(key));
        }

        return "%s%s".formatted(key, EXTENSION);
    }

    @Override
    protected String getKey(String fileName) {
        if (!fileName.endsWith(EXTENSION)) {
            return null;
        }

        String name = fileName.substring(0, fileName.length() - EXTENSION.length());

        if (!isValidName(name)) {
            Vio.getLogger().warn("Skipping script %s: invalid name, script names are %s".formatted(fileName, NAME_RULE));
            return null;
        }

        return name;
    }

    public static boolean isValidName(String name) {
        return NAME.matcher(name).matches() && !RESERVED_NAME.matcher(name).matches();
    }

    public List<String> getNames() {
        List<String> names = new ArrayList<>(getKeys());

        names.sort(Comparator.naturalOrder());
        return names;
    }
}
