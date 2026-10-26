package com.plummy.vio.core.storage;

import com.plummy.vio.Vio;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public abstract class GroovyDirectoryStorage<K, T extends Storage.Item<K>> implements Storage<K, T> {

    private final Path directory;
    private final Map<K, T> cache = new ConcurrentHashMap<>();

    protected GroovyDirectoryStorage(Path directory) {
        this.directory = directory;
    }

    protected abstract String serialize(T value);

    protected abstract T deserialize(K key, String source);

    protected abstract String getFileName(K key);

    protected abstract K getKey(String fileName);

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

            Path temporary = Files.createTempFile(directory, path.getFileName().toString(), ".tmp");
            Files.writeString(temporary, serialize(value), StandardCharsets.UTF_8);

            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to save %s".formatted(path), e);
        }
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

    public List<K> getKeys() {
        if (!Files.isDirectory(directory)) {
            return List.of();
        }

        List<K> keys = new ArrayList<>();

        try (Stream<Path> files = Files.list(directory)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                K key = getKey(file.getFileName().toString());

                if (key != null) {
                    keys.add(key);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to list %s".formatted(directory), e);
        }

        return keys;
    }

    protected Path getPath(K key) {
        String fileName = getFileName(key);
        Path path = directory.resolve(fileName);

        if (!directory.normalize().equals(path.normalize().getParent())) {
            throw new IllegalArgumentException("File name leaves the storage directory: %s".formatted(fileName));
        }

        return path;
    }

    protected T load(K key) {
        Path path = getPath(key);

        if (!Files.exists(path)) {
            return null;
        }

        try {
            return deserialize(key, Files.readString(path, StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException e) {
            Vio.getLogger().warn("Failed to load %s, ignoring it".formatted(path), e);
            return null;
        }
    }
}
