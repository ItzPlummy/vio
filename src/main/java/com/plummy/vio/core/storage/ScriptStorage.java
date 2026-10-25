package com.plummy.vio.core.storage;

import com.plummy.vio.Vio;
import com.plummy.vio.core.script.ScriptFile;
import com.plummy.vio.core.script.ScriptHeader;
import com.plummy.vio.core.script.ScriptType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

// One <name>.groovy file per script. The file is all that's saved: its header comment holds the metadata, e.g.
//
//   /* @vio
//   type: periodic
//   description: Heals sleeping players
//   interval_ticks: 20
//   enabled: true
//   creator: 0f8c6a7e-...
//   created_at: 2026-10-24T18:03:11Z
//   */
//   <source>
//
// The header is read as text, never compiled: "key: value" lines between the first line "/* @vio" and the first line
// "*/". Unknown, duplicate, missing or misplaced keys make the whole file invalid.
//
// Files are only read at server start, and toggling or replacing a script rewrites its whole file, so they should
// be edited by hand only while the server is stopped.
public class ScriptStorage implements Storage<String, ScriptFile> {

    public static final String NAME_RULE = "1 to 48 characters: lowercase letters, digits and underscores, starting with a letter";

    private static final String EXTENSION = ".groovy";
    private static final Pattern NAME = Pattern.compile("[a-z][a-z0-9_]{0,47}");
    // Windows can't create files with these names, whatever the extension.
    private static final Pattern RESERVED_NAME = Pattern.compile("con|prn|aux|nul|com[0-9]|lpt[0-9]");

    private static final String HEADER_START = "/* @vio";
    private static final String HEADER_END = "*/";
    private static final Pattern KEY = Pattern.compile("[a-z_]+");

    private static final String TYPE = "type";
    private static final String DESCRIPTION = "description";
    private static final String INTERVAL_TICKS = "interval_ticks";
    private static final String PERMISSION_LEVEL = "permission_level";
    private static final String ENABLED = "enabled";
    private static final String CREATOR = "creator";
    private static final String CREATED_AT = "created_at";
    private static final String TOGGLED_BY = "toggled_by";
    private static final String TOGGLED_AT = "toggled_at";
    private static final Set<String> KEYS = Set.of(TYPE, DESCRIPTION, INTERVAL_TICKS, PERMISSION_LEVEL, ENABLED, CREATOR, CREATED_AT, TOGGLED_BY, TOGGLED_AT);

    private final Path directory;

    public ScriptStorage(Path directory) {
        this.directory = directory;
    }

    public static ScriptStorage fromServer(MinecraftServer server) {
        return new ScriptStorage(server.getWorldPath(new LevelResource("serverconfig")).resolve(Vio.MODID).resolve("scripts"));
    }

    public static boolean isValidName(String name) {
        return NAME.matcher(name).matches() && !RESERVED_NAME.matcher(name).matches();
    }

    // Names of every script file, sorted. Files with invalid names are skipped, since nothing could address them.
    public List<String> getNames() {
        if (!Files.isDirectory(directory)) {
            return List.of();
        }

        List<String> names = new ArrayList<>();

        try (Stream<Path> files = Files.list(directory)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                String fileName = file.getFileName().toString();

                if (!fileName.endsWith(EXTENSION)) {
                    continue;
                }

                String name = fileName.substring(0, fileName.length() - EXTENSION.length());

                if (isValidName(name)) {
                    names.add(name);
                } else {
                    Vio.getLogger().warn("Skipping script {}: invalid name, script names are {}", file, NAME_RULE);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to list " + directory, e);
        }

        names.sort(Comparator.naturalOrder());
        return names;
    }

    // Null when the file is missing or invalid; invalid files are logged.
    @Override
    public ScriptFile get(String name) {
        Path path = getPath(name);

        if (!Files.exists(path)) {
            return null;
        }

        try {
            return deserialize(name, Files.readString(path, StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException e) {
            Vio.getLogger().warn("Failed to load script {}, ignoring it: {}", path, e.getMessage());
            return null;
        }
    }

    // True for invalid files too: their name is taken until they're replaced or deleted.
    @Override
    public boolean has(String name) {
        return Files.exists(getPath(name));
    }

    @Override
    public void save(ScriptFile value) {
        Path path = getPath(value.getName());

        try {
            AtomicFiles.writeString(path, serialize(value));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to save " + path, e);
        }
    }

    @Override
    public void delete(String name) {
        Path path = getPath(name);

        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to delete " + path, e);
        }
    }

    private Path getPath(String name) {
        if (!isValidName(name)) {
            throw new IllegalArgumentException("Invalid script name: " + name);
        }

        Path path = directory.resolve(name + EXTENSION);

        // The name rule already rules out separators and dots; this keeps it that way if the rule ever changes.
        if (!directory.normalize().equals(path.normalize().getParent())) {
            throw new IllegalArgumentException("Script name leaves the scripts directory: " + name);
        }

        return path;
    }

    private static String serialize(ScriptFile file) {
        ScriptHeader header = file.getHeader();
        String description = header.getDescription().replaceAll("\\R", " ");

        if (description.contains(HEADER_END)) {
            throw new IllegalArgumentException("A script description can't contain " + HEADER_END);
        }

        StringBuilder text = new StringBuilder();

        text.append(HEADER_START).append('\n');
        appendLine(text, TYPE, header.getType().getValue());
        appendLine(text, DESCRIPTION, description);

        if (header.getType() == ScriptType.PERIODIC) {
            appendLine(text, INTERVAL_TICKS, header.getIntervalTicks().toString());
        }

        if (header.getType() == ScriptType.COMMAND) {
            appendLine(text, PERMISSION_LEVEL, header.getPermissionLevel().toString());
        }

        if (header.getType().isToggleable()) {
            appendLine(text, ENABLED, Boolean.toString(header.isEnabled()));
        }

        appendLine(text, CREATOR, header.getCreator().toString());
        appendLine(text, CREATED_AT, header.getCreatedAt().toString());

        if (header.getToggledBy() != null) {
            appendLine(text, TOGGLED_BY, header.getToggledBy().toString());
            appendLine(text, TOGGLED_AT, header.getToggledAt().toString());
        }

        text.append(HEADER_END).append('\n');
        text.append(file.getSource());

        return text.toString();
    }

    private static void appendLine(StringBuilder text, String key, String value) {
        text.append(key).append(": ").append(value).append('\n');
    }

    private static ScriptFile deserialize(String name, String text) {
        if (text.startsWith("﻿")) {
            text = text.substring(1);
        }

        // Hand-edited files may have Windows line endings; they're written back with \n.
        String[] lines = text.split("\\R", -1);

        if (!lines[0].strip().equals(HEADER_START)) {
            throw new IllegalArgumentException("the file must start with the line " + HEADER_START);
        }

        int end = 1;

        while (end < lines.length && !lines[end].strip().equals(HEADER_END)) {
            end++;
        }

        if (end == lines.length) {
            throw new IllegalArgumentException("the header is never closed with a " + HEADER_END + " line");
        }

        Map<String, String> values = new HashMap<>();

        for (int i = 1; i < end; i++) {
            String line = lines[i];

            if (line.isBlank()) {
                continue;
            }

            int colon = line.indexOf(':');
            String key = colon < 0 ? "" : line.substring(0, colon).strip();

            if (!KEY.matcher(key).matches()) {
                throw new IllegalArgumentException("line %d: expected 'key: value', got \"%s\"".formatted(i + 1, line));
            }

            if (!KEYS.contains(key)) {
                throw new IllegalArgumentException("line %d: unknown key '%s'".formatted(i + 1, key));
            }

            if (values.put(key, line.substring(colon + 1).strip()) != null) {
                throw new IllegalArgumentException("line %d: duplicate key '%s'".formatted(i + 1, key));
            }
        }

        ScriptType type = ScriptType.fromValue(require(values, TYPE));

        checkPresence(values, INTERVAL_TICKS, type == ScriptType.PERIODIC, type);
        checkPresence(values, PERMISSION_LEVEL, type == ScriptType.COMMAND, type);
        checkPresence(values, ENABLED, type.isToggleable(), type);

        if (values.containsKey(TOGGLED_BY) != values.containsKey(TOGGLED_AT)) {
            throw new IllegalArgumentException("'%s' and '%s' must be both present or both absent".formatted(TOGGLED_BY, TOGGLED_AT));
        }

        ScriptHeader header = new ScriptHeader(
                type,
                require(values, DESCRIPTION),
                values.containsKey(INTERVAL_TICKS) ? parseInt(values, INTERVAL_TICKS, ScriptHeader.MIN_INTERVAL_TICKS, ScriptHeader.MAX_INTERVAL_TICKS) : null,
                values.containsKey(PERMISSION_LEVEL) ? parseInt(values, PERMISSION_LEVEL, 0, ScriptHeader.MAX_PERMISSION_LEVEL) : null,
                !values.containsKey(ENABLED) || parseBoolean(values, ENABLED),
                UUID.fromString(require(values, CREATOR)),
                Instant.parse(require(values, CREATED_AT)),
                values.containsKey(TOGGLED_BY) ? UUID.fromString(values.get(TOGGLED_BY)) : null,
                values.containsKey(TOGGLED_AT) ? Instant.parse(values.get(TOGGLED_AT)) : null
        );

        String source = String.join("\n", Arrays.asList(lines).subList(end + 1, lines.length));

        return new ScriptFile(name, header, source);
    }

    private static String require(Map<String, String> values, String key) {
        String value = values.get(key);

        if (value == null) {
            throw new IllegalArgumentException("missing key '%s'".formatted(key));
        }

        return value;
    }

    private static void checkPresence(Map<String, String> values, String key, boolean expected, ScriptType type) {
        if (expected && !values.containsKey(key)) {
            throw new IllegalArgumentException("missing key '%s', which %s scripts need".formatted(key, type));
        }

        if (!expected && values.containsKey(key)) {
            throw new IllegalArgumentException("key '%s' doesn't apply to %s scripts".formatted(key, type));
        }
    }

    private static int parseInt(Map<String, String> values, String key, int min, int max) {
        int value;

        try {
            value = Integer.parseInt(values.get(key));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("'%s' must be an integer, got \"%s\"".formatted(key, values.get(key)));
        }

        if (value < min || value > max) {
            throw new IllegalArgumentException("'%s' must be between %d and %d, got %d".formatted(key, min, max, value));
        }

        return value;
    }

    private static boolean parseBoolean(Map<String, String> values, String key) {
        return switch (values.get(key)) {
            case "true" -> true;
            case "false" -> false;
            default -> throw new IllegalArgumentException("'%s' must be true or false, got \"%s\"".formatted(key, values.get(key)));
        };
    }
}
