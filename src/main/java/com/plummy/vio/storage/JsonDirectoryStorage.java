package com.plummy.vio.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.plummy.vio.Vio;

public abstract class JsonDirectoryStorage<K, V extends StorageItem<K>> extends DirectoryStorage<K, V> {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    protected abstract String serializeKey(K key);

    protected abstract K deserializeKey(String value);

    protected abstract JsonObject serializeItemToJson(V item);

    protected abstract V deserializeItemFromJson(K key, JsonObject json);

    @Override
    protected String getFileNameFromKey(K key) {
        return "%s%s".formatted(serializeKey(key), ".json");
    }

    @Override
    protected K getKeyFromFileName(String fileName) {
        if (!fileName.endsWith(".json")) {
            return null;
        }

        String value = fileName.substring(0, fileName.length() - ".json".length());

        try {
            K key = deserializeKey(value);

            if (!serializeKey(key).equals(value)) {
                Vio.getLogger().warn("Skipping %s: its name isn't a canonical key".formatted(fileName));
                return null;
            }

            return key;
        } catch (RuntimeException e) {
            Vio.getLogger().warn("Skipping %s: its name isn't a valid key".formatted(fileName), e);
            return null;
        }
    }

    @Override
    protected String serializeItem(V item) {
        return GSON.toJson(serializeItemToJson(item));
    }

    @Override
    protected V deserializeItem(K key, String value) {
        return deserializeItemFromJson(key, JsonParser.parseString(value).getAsJsonObject());
    }
}
