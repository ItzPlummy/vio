package com.plummy.vio.storage;

import com.plummy.vio.core.Keyed;

public interface Storage<K, T extends Keyed<K>> {

    T get(K key);

    boolean has(K key);

    void save(T value);

    void delete(K key);
}
