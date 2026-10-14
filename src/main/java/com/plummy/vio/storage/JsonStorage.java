package com.plummy.vio.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.plummy.vio.Vio;
import com.plummy.vio.core.Keyed;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class JsonStorage<K, T extends Keyed<K>> implements Storage<K, T> {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path directory;
    private final Map<K, T> cache = new ConcurrentHashMap<>();

    protected JsonStorage(Path directory) {
        this.directory = directory;
    }

    protected abstract JsonObject serialize(T value);

    protected abstract T deserialize(JsonObject json);

    protected abstract String getFileName(K key);

    @Override
    public T get(K key) {
        return cache.computeIfAbsent(key, this::load);
    }

    @Override
    public boolean has(K key) {
        return get(key) != null;
    }

    @Override
    public void save(T value) {
        cache.put(value.getKey(), value);

        Path path = getPath(value.getKey());

        try {
            Files.createDirectories(directory);

            Path temporary = Files.createTempFile(directory, getFileName(value.getKey()), ".tmp");
            Files.writeString(temporary, GSON.toJson(serialize(value)), StandardCharsets.UTF_8);

            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to save " + path, e);
        }
    }

    @Override
    public void delete(K key) {
        cache.remove(key);

        Path path = getPath(key);

        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to delete " + path, e);
        }
    }

    protected Path getPath(K key) {
        return directory.resolve(getFileName(key));
    }

    protected T load(K key) {
        Path path = getPath(key);

        if (!Files.exists(path)) {
            return null;
        }

        try {
            return deserialize(JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject());
        } catch (IOException | RuntimeException e) {
            Vio.getLogger().warn("Failed to load {}, ignoring it", path, e);
            return null;
        }
    }
}
