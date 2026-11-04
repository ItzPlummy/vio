package com.plummy.vio.core.storage;

import java.util.Collection;

public interface Storage<K, V> {

    V get(K key);

    Collection<K> getKeys();

    Collection<V> getItems();

    int getSize();

    boolean has(K key);

    void save(V item);

    void delete(K key);

    void clear();
}
