package com.plummy.vio.datapack;

import com.plummy.vio.datapack.type.DatapackFileType;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public interface DatapackManager {

    Collection<DatapackPath> getPaths();

    DatapackFile getFile(DatapackPath path);

    GameDatapackFile getGameFile(DatapackPath path);

    DatapackFileType getFileType(DatapackPath path);

    DatapackWriteResult writeFile(DatapackFile file);

    boolean deleteFile(DatapackPath path);

    CompletableFuture<Void> apply();
}
