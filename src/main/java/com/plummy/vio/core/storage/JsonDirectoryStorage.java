package com.plummy.vio.core.storage;

import com.google.gson.JsonObject;
import com.plummy.vio.Vio;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class JsonDirectoryStorage<K, T extends Storage.Item<K>> extends AbstractJsonStorage<K, T> {

    private final Path directory;
    private final Map<K, T> cache = new ConcurrentHashMap<>();

    protected JsonDirectoryStorage(Path directory) {
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
        writeJson(getPath(value.getKey()), serialize(value));
    }

    @Override
    public void delete(K key) {
        cache.remove(key);

        Path path = getPath(key);

        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to delete %s".formatted(path), e);
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
            return deserialize(readJson(path));
        } catch (IOException | RuntimeException e) {
            Vio.getLogger().warn("Failed to load %s, ignoring it".formatted(path), e);
            return null;
        }
    }
}
