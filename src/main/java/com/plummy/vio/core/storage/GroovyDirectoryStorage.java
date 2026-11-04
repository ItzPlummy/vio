package com.plummy.vio.core.storage;

import com.plummy.vio.Vio;

public abstract class GroovyDirectoryStorage<K, V> extends DirectoryStorage<K, V> {

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
            return deserializeKey(value);
        } catch (RuntimeException e) {
            Vio.getLogger().warn("Skipping %s: its name isn't a valid key".formatted(fileName), e);
            return null;
        }
    }
}
