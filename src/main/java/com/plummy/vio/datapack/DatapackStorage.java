package com.plummy.vio.datapack;

import com.plummy.vio.Vio;
import com.plummy.vio.storage.Storage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class DatapackStorage implements Storage<DatapackPath, DatapackFile> {

    private final Path root;

    public DatapackStorage(Path root) {
        this.root = root;
    }

    public static DatapackStorage fromServer(MinecraftServer server) {
        return new DatapackStorage(getRoot(server.getWorldPath(new LevelResource("serverconfig"))));
    }

    public static Path getRoot(Path serverConfig) {
        return serverConfig.resolve(Vio.MODID).resolve("datapack");
    }

    public Path getRoot() {
        return root;
    }

    public Path getDataPath() {
        return root.resolve("data");
    }

    @Override
    public synchronized DatapackFile get(DatapackPath key) {
        Path path = getItemPath(key);

        if (!Files.isRegularFile(path)) {
            return null;
        }

        try {
            return new DatapackFile(key, Files.readString(path, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read %s".formatted(path), e);
        }
    }

    @Override
    public synchronized Collection<DatapackPath> getKeys() {
        Path data = getDataPath();

        if (!Files.isDirectory(data)) {
            return List.of();
        }

        try (Stream<Path> stream = Files.walk(data)) {
            return stream.filter(Files::isRegularFile)
                    .filter(path -> !path.getFileName().toString().endsWith(".tmp"))
                    .map(path -> getKeyFromPath(data.relativize(path)))
                    .filter(Objects::nonNull)
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to list %s".formatted(data), e);
        }
    }

    @Override
    public synchronized Collection<DatapackFile> getItems() {
        List<DatapackFile> items = new ArrayList<>();

        for (DatapackPath key : getKeys()) {
            DatapackFile item = get(key);

            if (item != null) {
                items.add(item);
            }
        }

        return items;
    }

    @Override
    public synchronized int getSize() {
        return getKeys().size();
    }

    @Override
    public synchronized boolean has(DatapackPath key) {
        return Files.isRegularFile(getItemPath(key));
    }

    @Override
    public synchronized void save(DatapackFile item) {
        Path path = getItemPath(item.getKey());

        try {
            Files.createDirectories(path.getParent());
            Path temporary = Files.createTempFile(path.getParent(), path.getFileName().toString(), ".tmp");

            try {
                Files.writeString(temporary, item.getContent(), StandardCharsets.UTF_8);

                try {
                    Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException | RuntimeException e) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException suppressed) {
                    e.addSuppressed(suppressed);
                }

                throw e;
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to save %s".formatted(path), e);
        }
    }

    @Override
    public synchronized void delete(DatapackPath key) {
        Path path = getItemPath(key);

        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to delete %s".formatted(path), e);
        }

        deleteEmptyParents(path.getParent());
    }

    @Override
    public synchronized void clear() {
        for (DatapackPath key : getKeys()) {
            delete(key);
        }
    }

    private void deleteEmptyParents(Path directory) {
        Path data = getDataPath().normalize();

        for (Path current = directory.normalize(); current.startsWith(data) && !current.equals(data); current = current.getParent()) {
            try (Stream<Path> children = Files.list(current)) {
                if (children.findAny().isPresent()) {
                    return;
                }
            } catch (IOException e) {
                return;
            }

            try {
                Files.delete(current);
            } catch (IOException e) {
                Vio.getLogger().warn("Failed to delete the empty folder %s".formatted(current), e);
                return;
            }
        }
    }

    private Path getItemPath(DatapackPath key) {
        Path data = getDataPath().normalize();
        Path path = data.resolve(key.getNamespace()).resolve(key.getPath()).normalize();

        if (!path.startsWith(data) || path.equals(data)) {
            throw new DatapackException("Path '%s' isn't inside the data pack".formatted(key));
        }

        return path;
    }

    private static DatapackPath getKeyFromPath(Path relative) {
        if (relative.getNameCount() < 2) {
            return null;
        }

        List<String> names = new ArrayList<>();

        for (Path name : relative.subpath(1, relative.getNameCount())) {
            names.add(name.toString());
        }

        try {
            return new DatapackPath(relative.getName(0).toString(), String.join("/", names));
        } catch (DatapackException e) {
            return null;
        }
    }
}
