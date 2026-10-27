package com.plummy.vio.storage;

import com.plummy.vio.Vio;

public abstract class GroovyDirectoryStorage<K, V extends StorageItem<K>> extends DirectoryStorage<K, V> {

    protected abstract String serializeKey(K key);

    protected abstract K deserializeKey(String value);

    @Override
    protected String getFileNameFromKey(K key) {
        return "%s%s".formatted(serializeKey(key), ".groovy");
    }

    @Override
    protected K getKeyFromFileName(String fileName) {
        if (!fileName.endsWith(".groovy")) {
            return null;
        }

        String value = fileName.substring(0, fileName.length() - ".groovy".length());

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
}
