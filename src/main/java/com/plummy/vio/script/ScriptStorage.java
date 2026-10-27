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

    private static final Pattern NAME =Pattern.compile("[a-z][a-z0-9_]{0,47}");
    private static final Pattern RESERVED_NAME = Pattern.compile("con|prn|aux|nul|com[0-9]|lpt[0-9]");

    private final Path directory;

    public ScriptStorage(Path directory) {
        this.directory = directory;
    }

    public static ScriptStorage fromServer(MinecraftServer server) {
        return new ScriptStorage(server.getWorldPath(new LevelResource("serverconfig")).resolve(Vio.MODID).resolve("scripts"));
    }

    @Override
    public Path getPath() {
        return directory;
    }

    @Override
    protected String serializeKey(String key) {
        return requireValidName(key);
    }

    @Override
    protected String deserializeKey(String value) {
        return requireValidName(value);
    }

    @Override
    protected String serializeItem(Script item) {
        return item.accept(ScriptSerializer.getInstance());
    }

    @Override
    protected Script deserializeItem(String key, String value) {
        return ScriptDeserializer.getInstance().deserialize(key, value);
    }

    public static boolean isValidName(String name) {
        return NAME.matcher(name).matches() && !RESERVED_NAME.matcher(name).matches();
    }

    public List<String> getNames() {
        List<String> names = new ArrayList<>(getKeys());

        names.sort(Comparator.naturalOrder());
        return names;
    }

    private static String requireValidName(String name) {
        if (!isValidName(name)) {
            throw new IllegalArgumentException("Invalid script name '%s': names are %s".formatted(name, NAME_RULE));
        }

        return name;
    }
}
