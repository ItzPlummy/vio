package com.plummy.vio.config;

import com.plummy.vio.Vio;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class Instructions {

    private static final String BUNDLED_RESOURCE = "/vio/instructions.txt";

    private final String text;

    private Instructions(String text) {
        this.text = text;
    }

    public static Path getPath() {
        return FMLPaths.CONFIGDIR.get().resolve(Vio.MODID).resolve("instructions.txt");
    }

    public static Instructions load() {
        Path path = getPath();

        createDefaultIfMissing(path);

        try {
            String text = Files.readString(path, StandardCharsets.UTF_8);
            Vio.getLogger().info("Loaded instructions from {}", path);
            return new Instructions(text);
        } catch (IOException e) {
            Vio.getLogger().warn("Failed to read {}, using the bundled instructions", path, e);
            return new Instructions(readBundled());
        }
    }

    public String get() {
        return text;
    }

    private static void createDefaultIfMissing(Path path) {
        if (Files.exists(path)) {
            return;
        }

        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, readBundled(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            Vio.getLogger().warn("Failed to create {}", path, e);
        }
    }

    private static String readBundled() {
        try (InputStream stream = Instructions.class.getResourceAsStream(BUNDLED_RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("Bundled instructions " + BUNDLED_RESOURCE + " are missing");
            }

            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read bundled instructions " + BUNDLED_RESOURCE, e);
        }
    }
}
