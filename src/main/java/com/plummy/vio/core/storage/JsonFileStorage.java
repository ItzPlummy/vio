package com.plummy.vio.core.storage;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.plummy.vio.Vio;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class JsonFileStorage<K, T extends Storage.Item<K>> extends AbstractJsonStorage<K, T> {

    private final Path file;
    private Map<K, T> items;

    protected JsonFileStorage(Path file) {
        this.file = file;
    }

    protected abstract JsonObject serialize(T value);

    protected abstract T deserialize(K key, JsonObject json);

    protected abstract K keyFromString(String key);

    protected abstract String keyToString(K key);

    @Override
    public synchronized T get(K key) {
        return getItems().get(key);
    }

    @Override
    public synchronized boolean has(K key) {
        return getItems().containsKey(key);
    }

    @Override
    public synchronized void save(T value) {
        getItems().put(value.getKey(), value);
        write();
    }

    @Override
    public synchronized void delete(K key) {
        if (getItems().remove(key) != null) {
            write();
        }
    }

    private Map<K, T> getItems() {
        if (items == null) {
            items = load();
        }

        return items;
    }

    private Map<K, T> load() {
        Map<K, T> loaded = new LinkedHashMap<>();

        if (!Files.exists(file)) {
            return loaded;
        }

        try {
            for (Map.Entry<String, JsonElement> entry : readJson(file).entrySet()) {
                K key = keyFromString(entry.getKey());
                loaded.put(key, deserialize(key, entry.getValue().getAsJsonObject()));
            }

            return loaded;
        } catch (IOException | RuntimeException e) {
            Path broken = file.resolveSibling(file.getFileName() + ".broken");
            Vio.getLogger().error("Failed to load %s, moving it to %s and starting empty".formatted(file, broken), e);

            try {
                Files.move(file, broken, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException moveException) {
                Vio.getLogger().error("Failed to move %s to %s".formatted(file, broken), moveException);
            }

            return new LinkedHashMap<>();
        }
    }

    private void write() {
        JsonObject json = new JsonObject();

        for (Map.Entry<K, T> entry : items.entrySet()) {
            json.add(keyToString(entry.getKey()), serialize(entry.getValue()));
        }

        writeJson(file, json);
    }
}
