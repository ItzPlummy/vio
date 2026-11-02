package com.plummy.vio.datapack;

import com.plummy.vio.storage.StorageItem;

public final class DatapackFile implements StorageItem<DatapackPath> {

    private final DatapackPath path;
    private final String content;

    public DatapackFile(DatapackPath path, String content) {
        this.path = path;
        this.content = content;
    }

    @Override
    public DatapackPath getKey() {
        return getPath();
    }

    public DatapackPath getPath() {
        return path;
    }

    public String getContent() {
        return content;
    }
}
