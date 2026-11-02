package com.plummy.vio.script;

import com.plummy.vio.storage.StorageItem;

public class ScriptSource implements StorageItem<ScriptKey> {

    private final ScriptKey key;
    private final String source;

    public ScriptSource(ScriptKey key, String source) {
        this.key = key;
        this.source = source;
    }

    @Override
    public ScriptKey getKey() {
        return key;
    }

    public String getSource() {
        return source;
    }
}
