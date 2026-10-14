package com.plummy.vio.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public abstract class AbstractJsonStorage<K, T extends Storage.Item<K>> implements Storage<K, T> {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    protected JsonObject readJson(Path path) throws IOException {
        return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    protected void writeJson(Path path, JsonObject json) {
        Path directory = path.getParent();

        try {
            Files.createDirectories(directory);

            Path temporary = Files.createTempFile(directory, path.getFileName().toString(), ".tmp");
            Files.writeString(temporary, GSON.toJson(json), StandardCharsets.UTF_8);

            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to save " + path, e);
        }
    }
}
