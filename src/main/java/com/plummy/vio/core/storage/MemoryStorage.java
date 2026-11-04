package com.plummy.vio.core.storage;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class MemoryStorage<K, V> implements Storage<K, V> {

    private final Map<K, V> items = new LinkedHashMap<>();

    protected abstract K getKey(V item);

    @Override
    public synchronized V get(K key) {
        return items.get(key);
    }

    @Override
    public synchronized Collection<K> getKeys() {
        return List.copyOf(items.keySet());
    }

    @Override
    public synchronized Collection<V> getItems() {
        return List.copyOf(items.values());
    }

    @Override
    public synchronized int getSize() {
        return items.size();
    }

    @Override
    public synchronized boolean has(K key) {
        return items.containsKey(key);
    }

    @Override
    public synchronized void save(V item) {
        items.put(getKey(item), item);
    }

    @Override
    public synchronized void delete(K key) {
        items.remove(key);
    }

    @Override
    public synchronized void clear() {
        items.clear();
    }
}
