package com.plummy.vio.core.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

public abstract class FileStorage<K, V> implements Storage<K, V> {

    private final Map<K, V> cache = new LinkedHashMap<>();
    private boolean isLoaded = false;

    public abstract Path getPath();

    protected abstract K getKey(V item);

    protected abstract String serializeItems(Collection<V> items);

    protected abstract Collection<V> deserializeItems(String value);

    @Override
    public synchronized V get(K key) {
        requireLoaded();
        return cache.get(key);
    }

    @Override
    public synchronized Collection<K> getKeys() {
        requireLoaded();
        return List.copyOf(cache.keySet());
    }

    @Override
    public synchronized Collection<V> getItems() {
        requireLoaded();
        return List.copyOf(cache.values());
    }

    @Override
    public synchronized int getSize() {
        requireLoaded();
        return cache.size();
    }

    @Override
    public synchronized boolean has(K key) {
        requireLoaded();
        return cache.containsKey(key);
    }

    @Override
    public synchronized void save(V item) {
        requireLoaded();

        List<V> items = new ArrayList<>(cache.values());
        items.removeIf(i -> getKey(i).equals(getKey(item)));
        items.add(item);

        write(items);
        cache.put(getKey(item), item);
    }

    @Override
    public synchronized void delete(K key) {
        requireLoaded();

        if (!cache.containsKey(key)) {
            return;
        }

        List<V> items = new ArrayList<>(cache.values());
        items.removeIf(item -> getKey(item).equals(key));

        write(items);
        cache.remove(key);
    }

    @Override
    public synchronized void clear() {
        write(List.of());
        cache.clear();
        isLoaded = true;
    }

    protected void load() {
        Path path = getPath();

        if (Files.exists(path)) {
            try {
                for (V item : deserializeItems(Files.readString(path, StandardCharsets.UTF_8))) {
                    cache.put(getKey(item), item);
                }
            } catch (IOException e) {
                cache.clear();
                throw new UncheckedIOException("Failed to load %s".formatted(path), e);
            } catch (RuntimeException e) {
                cache.clear();
                throw new RuntimeException("Failed to load %s".formatted(path), e);
            }
        }

        isLoaded = true;
    }

    protected void write(Collection<V> items) {
        Path path = getPath();

        try {
            Path directory = path.toAbsolutePath().getParent();
            Files.createDirectories(directory);

            Path temporary = Files.createTempFile(directory, path.getFileName().toString(), ".tmp");

            try {
                Files.writeString(temporary, serializeItems(items), StandardCharsets.UTF_8);

                try {
                    Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException | RuntimeException e) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException suppressed) {
                    e.addSuppressed(suppressed);
                }

                throw e;
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to save %s".formatted(path), e);
        }
    }

    protected void requireLoaded() {
        if (!isLoaded) {
            load();
        }
    }
}
