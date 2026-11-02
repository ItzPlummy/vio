package com.plummy.vio.storage;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MemoryStorage<K, V extends StorageItem<K>> implements Storage<K, V> {

    private final Map<K, V> items = new ConcurrentHashMap<>();

    @Override
    public V get(K key) {
        return items.get(key);
    }

    @Override
    public Collection<K> getKeys() {
        return List.copyOf(items.keySet());
    }

    @Override
    public Collection<V> getItems() {
        return List.copyOf(items.values());
    }

    @Override
    public int getSize() {
        return items.size();
    }

    @Override
    public boolean has(K key) {
        return items.containsKey(key);
    }

    @Override
    public void save(V item) {
        items.put(item.getKey(), item);
    }

    @Override
    public void delete(K key) {
        items.remove(key);
    }

    @Override
    public void clear() {
        items.clear();
    }
}
