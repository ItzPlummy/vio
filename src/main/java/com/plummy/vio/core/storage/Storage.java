package com.plummy.vio.core.storage;

public interface Storage<K, T extends Storage.Item<K>> {

    T get(K key);

    boolean has(K key);

    void save(T value);

    void delete(K key);

    public static interface Item<T> {

        T getKey();
    }
}
