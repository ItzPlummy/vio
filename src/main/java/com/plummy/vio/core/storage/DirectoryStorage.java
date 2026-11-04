package com.plummy.vio.core.storage;

import com.plummy.vio.Vio;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Stream;

public abstract class DirectoryStorage<K, V> implements Storage<K, V> {

    private final Map<K, V> cache = new LinkedHashMap<>();
    private final Set<K> missing = new HashSet<>();
    private boolean synced = false;

    public abstract Path getPath();

    protected abstract K getKey(V item);

    protected abstract String getFileNameFromKey(K key);

    protected abstract K getKeyFromFileName(String fileName);

    protected abstract String serializeItem(V item);

    protected abstract V deserializeItem(K key, String value);

    @Override
    public synchronized V get(K key) {
        V item = cache.get(key);

        if (item != null || synced || missing.contains(key)) {
            return item;
        }

        item = getPersistent(key);

        if (item == null) {
            missing.add(key);
        } else {
            cache.put(key, item);
        }

        return item;
    }

    @Override
    public synchronized Collection<K> getKeys() {
        if (!synced) {
            sync();
        }

        return List.copyOf(cache.keySet());
    }

    @Override
    public synchronized Collection<V> getItems() {
        if (!synced) {
            sync();
        }

        return List.copyOf(cache.values());
    }

    @Override
    public synchronized int getSize() {
        if (!synced) {
            sync();
        }

        return cache.size();
    }

    @Override
    public synchronized boolean has(K key) {
        return get(key) != null;
    }

    @Override
    public synchronized void save(V item) {
        savePersistent(item);
        cache.put(getKey(item), item);
        missing.remove(getKey(item));
    }

    @Override
    public synchronized void delete(K key) {
        deletePersistent(key);
        cache.remove(key);

        if (!synced) {
            missing.add(key);
        }
    }

    @Override
    public synchronized void clear() {
        for (K key : getPersistentKeys()) {
            delete(key);
        }

        cache.clear();
        missing.clear();
        synced = true;
    }

    public void sync() {
        if (synced) {
            return;
        }

        for (K key : getPersistentKeys()) {
            if (cache.containsKey(key) || missing.contains(key)) {
                continue;
            }

            V item = getPersistent(key);

            if (item != null) {
                cache.put(getKey(item), item);
            }
        }

        missing.clear();
        synced = true;
    }

    protected V getPersistent(K key) {
        Path path = getItemPath(key);

        if (!Files.exists(path)) {
            return null;
        }

        try {
            V item = deserializeItem(key, Files.readString(path, StandardCharsets.UTF_8));

            if (!key.equals(getKey(item))) {
                throw new IllegalStateException("Item key '%s' doesn't match its file name".formatted(getKey(item)));
            }

            return item;
        } catch (IOException | RuntimeException e) {
            Vio.getLogger().warn("Failed to load %s, ignoring it".formatted(path), e);
            return null;
        }
    }

    protected Collection<K> getPersistentKeys() {
        if (!Files.isDirectory(getPath())) {
            return List.of();
        }

        try (Stream<Path> stream = Files.list(getPath())) {
            return stream.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(fileName -> !fileName.endsWith(".tmp"))
                    .map(this::getKeyFromFileName)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to list %s".formatted(getPath()), e);
        }
    }

    protected void savePersistent(V item) {
        Path path = getItemPath(getKey(item));

        try {
            Files.createDirectories(getPath());
            Path temporary = Files.createTempFile(getPath(), path.getFileName().toString(), ".tmp");

            try {
                Files.writeString(temporary, serializeItem(item), StandardCharsets.UTF_8);

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

    protected void deletePersistent(K key) {
        Path path = getItemPath(key);

        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to delete %s".formatted(path), e);
        }
    }

    private Path getItemPath(K key) {
        String fileName = getFileNameFromKey(key);
        Path path = getPath().resolve(fileName);

        if (!getPath().normalize().equals(path.normalize().getParent())) {
            throw new IllegalArgumentException("File name '%s' isn't inside %s".formatted(fileName, getPath()));
        }

        return path;
    }
}
