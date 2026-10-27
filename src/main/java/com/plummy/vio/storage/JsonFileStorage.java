package com.plummy.vio.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Collection;

public abstract class JsonFileStorage<K, V extends StorageItem<K>> extends FileStorage<K, V> {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    protected abstract String serializeKey(K key);

    protected abstract K deserializeKey(String value);

    protected abstract JsonObject serializeItemToJson(V item);

    protected abstract V deserializeItemFromJson(K key, JsonObject json);

    @Override
    protected String serializeItems(Collection<V> items) {
        JsonObject json = new JsonObject();

        for (V item : items) {
            json.add(serializeKey(item.getKey()), serializeItemToJson(item));
        }

        return GSON.toJson(json);
    }

    @Override
    protected Collection<V> deserializeItems(String value) {
        JsonObject json = JsonParser.parseString(value).getAsJsonObject();

        return json.entrySet().stream()
                .filter(entry -> entry.getValue().isJsonObject())
                .map(entry -> deserializeItemFromJson(deserializeKey(entry.getKey()), entry.getValue().getAsJsonObject()))
                .toList();
    }
}
