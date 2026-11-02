package com.plummy.vio.datapack;

import com.plummy.vio.datapack.type.DatapackFileType;
import com.plummy.vio.datapack.type.DatapackFileTypes;
import com.plummy.vio.datapack.type.DatapackTarget;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Function;
import java.util.function.Supplier;

public final class DefaultDatapackManager implements DatapackManager {

    private final MinecraftServer server;
    private final DatapackStorage storage;
    private final DatapackFileTypes fileTypes;

    public DefaultDatapackManager(MinecraftServer server) {
        this.server = server;
        this.storage = DatapackStorage.fromServer(server);
        this.fileTypes = new DatapackFileTypes(server);
    }

    @Override
    public Collection<DatapackPath> getPaths() {
        return onServerThread(() -> List.copyOf(storage.getKeys()));
    }

    @Override
    public DatapackFile getFile(DatapackPath path) {
        return onServerThread(() -> storage.get(path));
    }

    @Override
    public GameDatapackFile getGameFile(DatapackPath path) {
        return onServerThread(() -> {
            Optional<Resource> resource = server.getResourceManager().getResource(ResourceLocation.fromNamespaceAndPath(path.getNamespace(), path.getPath()));

            if (resource.isEmpty()) {
                return null;
            }

            try (InputStream stream = resource.get().open()) {
                return new GameDatapackFile(new String(stream.readAllBytes(), StandardCharsets.UTF_8), resource.get().sourcePackId());
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to read %s".formatted(path), e);
            }
        });
    }

    @Override
    public DatapackFileType getFileType(DatapackPath path) {
        return onServerThread(() -> fileTypes.find(path));
    }

    @Override
    public DatapackWriteResult writeFile(DatapackFile file) {
        return onServerThread(() -> {
            DatapackTarget target = fileTypes.resolve(file.getPath());
            DatapackValidation validation = target.type().validate(server, target.id(), file.getContent());

            if (!validation.isValid()) {
                throw new DatapackException(String.join("\n", validation.errors()));
            }

            storage.save(file);
            return new DatapackWriteResult(!target.type().isReloadable(), validation.warnings());
        });
    }

    @Override
    public boolean deleteFile(DatapackPath path) {
        return onServerThread(() -> {
            if (!storage.has(path)) {
                throw new DatapackException("There's no file '%s' in Vio's data pack. Use list_datapack_files to see every file".formatted(path));
            }

            DatapackFileType type = fileTypes.find(path);
            storage.delete(path);

            return type == null || !type.isReloadable();
        });
    }

    @Override
    public CompletableFuture<Void> apply() {
        return server.submit(() -> server.reloadResources(server.getPackRepository().getSelectedIds())).thenCompose(Function.identity());
    }

    private <T> T onServerThread(Supplier<T> action) {
        if (server.isSameThread()) {
            return action.get();
        }

        try {
            return server.submit(action).join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof RuntimeException cause) {
                throw cause;
            }

            if (e.getCause() instanceof Error cause) {
                throw cause;
            }

            throw e;
        }
    }
}
