package com.plummy.vio.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class FileStorage<K, V extends StorageItem<K>> implements Storage<K, V> {

    private final MemoryStorage<K, V> cache = new MemoryStorage<>();
    private boolean isLoaded = false;

    public abstract Path getPath();

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
        return cache.getKeys();
    }

    @Override
    public synchronized Collection<V> getItems() {
        requireLoaded();
        return cache.getItems();
    }

    @Override
    public synchronized int getSize() {
        requireLoaded();
        return cache.getSize();
    }

    @Override
    public synchronized boolean has(K key) {
        requireLoaded();
        return cache.has(key);
    }

    @Override
    public synchronized void save(V item) {
        requireLoaded();

        List<V> items = new ArrayList<>(cache.getItems());
        items.removeIf(i -> i.getKey().equals(item.getKey()));
        items.add(item);

        write(items);
        cache.save(item);
    }

    @Override
    public synchronized void delete(K key) {
        requireLoaded();

        if (!cache.has(key)) {
            return;
        }

        List<V> items = new ArrayList<>(cache.getItems());
        items.removeIf(item -> item.getKey().equals(key));

        write(items);
        cache.delete(key);
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
                    cache.save(item);
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
        if (isLoaded) return;
        load();
    }
}
