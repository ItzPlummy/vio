package com.plummy.vio.datapack;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Objects;

public class DatapackPath {

    public static final String RULE = "<namespace>/<folder>/<name>.<json or mcfunction>, with lowercase letters, digits, underscores, dashes and dots, and slashes between folders";

    private static final List<String> EXTENSIONS = List.of("json", "mcfunction");

    private final String namespace;
    private final String path;

    public DatapackPath(String namespace, String path) {
        if (namespace == null || namespace.isEmpty() || !ResourceLocation.isValidNamespace(namespace)) {
            throw new DatapackException("Invalid namespace '%s': paths are %s".formatted(namespace, RULE));
        }

        if (path == null || path.isEmpty() || !ResourceLocation.isValidPath(path)) {
            throw new DatapackException("Invalid path '%s': paths are %s".formatted(path, RULE));
        }

        for (String segment : path.split("/", -1)) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
                throw new DatapackException("Invalid path '%s': empty, '.' and '..' folders aren't allowed".formatted(path));
            }
        }

        if (EXTENSIONS.stream().noneMatch(extension -> path.endsWith(".%s".formatted(extension)))) {
            throw new DatapackException("Invalid path '%s': files end in .json, or .mcfunction for functions".formatted(path));
        }

        this.namespace = namespace;
        this.path = path;
    }

    public static DatapackPath parse(String value) {
        int slash = value == null ? -1 : value.indexOf('/');

        if (slash < 0) {
            throw new DatapackException("Invalid path '%s': paths are %s".formatted(value, RULE));
        }

        return new DatapackPath(value.substring(0, slash), value.substring(slash + 1));
    }

    public String getNamespace() {
        return namespace;
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return "%s/%s".formatted(getNamespace(), getPath());
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof DatapackPath datapackPath)) {
            return false;
        }

        return getNamespace().equals(datapackPath.getNamespace()) && getPath().equals(datapackPath.getPath());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getNamespace(), getPath());
    }
}
