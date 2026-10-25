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

    private static final String PATH = "/vio/instructions.txt";

    private final String text;

    private Instructions(String text) {
        this.text = text;
    }

    public static Instructions load() {
        Path path = FMLPaths.CONFIGDIR.get().resolve(PATH);

        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path.getParent());
                Files.writeString(path, getDefaultInstructions(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                Vio.getLogger().warn("Failed to create agent instructions", e);
            }
        }

        try {
            String text = Files.readString(path, StandardCharsets.UTF_8);
            return new Instructions(text);
        } catch (IOException e) {
            Vio.getLogger().warn("Failed to read agent instructions", e);
            return new Instructions(getDefaultInstructions());
        }
    }

    public String getText() {
        return text;
    }

    private static String getDefaultInstructions() {
        try (InputStream stream = Instructions.class.getResourceAsStream(PATH)) {
            if (stream == null) {
                throw new IllegalStateException("Default instructions are missing");
            }

            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read default instructions", e);
        }
    }
}
