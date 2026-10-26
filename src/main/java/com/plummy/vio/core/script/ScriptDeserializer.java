package com.plummy.vio.core.script;

import com.plummy.vio.config.ScriptLimits;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

public class ScriptDeserializer {

    private static final ScriptDeserializer INSTANCE = new ScriptDeserializer();

    private static final Pattern KEY = Pattern.compile("[a-z_]+");
    private static final Set<String> KEYS = Set.of("type", "description", "creator", "created_at", "interval_ticks", "permission_level", "enabled", "toggled_by", "toggled_at");

    public static ScriptDeserializer getInstance() {
        return INSTANCE;
    }

    private ScriptDeserializer() {
    }

    public Script deserialize(String name, String text) {
        if (text.startsWith("﻿")) {
            text = text.substring(1);
        }

        String[] lines = text.split("\\R", -1);

        if (!lines[0].strip().equals(ScriptSerializer.HEADER_START)) {
            throw new IllegalArgumentException("the file must start with the line %s".formatted(ScriptSerializer.HEADER_START));
        }

        int end = 1;

        while (end < lines.length && !lines[end].strip().equals(ScriptSerializer.HEADER_END)) {
            end++;
        }

        if (end == lines.length) {
            throw new IllegalArgumentException("the header is never closed with a %s line".formatted(ScriptSerializer.HEADER_END));
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
                throw new IllegalArgumentException("line %s: expected 'key: value', got '%s'".formatted(i + 1, line));
            }

            if (!KEYS.contains(key)) {
                throw new IllegalArgumentException("line %s: unknown key '%s'".formatted(i + 1, key));
            }

            if (values.put(key, line.substring(colon + 1).strip()) != null) {
                throw new IllegalArgumentException("line %s: duplicate key %s".formatted(i + 1, key));
            }
        }

        ScriptType type = ScriptType.fromValue(require(values, "type"));

        checkPresence(values, "interval_ticks", type == ScriptType.PERIODIC, type);
        checkPresence(values, "permission_level", type == ScriptType.COMMAND, type);
        checkPresence(values, "enabled", type.isToggleable(), type);
        checkPresence(values, "toggled_by", type.isToggleable(), type);
        checkPresence(values, "toggled_at", type.isToggleable(), type);

        String source = String.join("\n", Arrays.asList(lines).subList(end + 1, lines.length));
        Script script = switch (type) {
            case ONE_SHOT -> new OneShotScript(
                    name,
                    require(values, "description"),
                    UUID.fromString(require(values, "creator")),
                    Instant.parse(require(values, "created_at")),
                    source
            );
            case PERIODIC -> new PeriodicScript(
                    name,
                    require(values, "description"),
                    UUID.fromString(require(values, "creator")),
                    Instant.parse(require(values, "created_at")),
                    parseInt(values, "interval_ticks", ScriptLimits.MIN_INTERVAL_TICKS, ScriptLimits.MAX_INTERVAL_TICKS),
                    parseBoolean(values, "enabled"),
                    UUID.fromString(require(values, "toggled_by")),
                    Instant.parse(require(values, "toggled_at")),
                    source
            );
            case LISTENER -> new ListenerScript(
                    name,
                    require(values, "description"),
                    UUID.fromString(require(values, "creator")),
                    Instant.parse(require(values, "created_at")),
                    parseBoolean(values, "enabled"),
                    UUID.fromString(require(values, "toggled_by")),
                    Instant.parse(require(values, "toggled_at")),
                    source
            );
            case COMMAND -> new CommandScript(
                    name,
                    require(values, "description"),
                    UUID.fromString(require(values, "creator")),
                    Instant.parse(require(values, "created_at")),
                    parseInt(values, "permission_level", 0, CommandScript.MAX_PERMISSION_LEVEL),
                    parseBoolean(values, "enabled"),
                    UUID.fromString(require(values, "toggled_by")),
                    Instant.parse(require(values, "toggled_at")),
                    source
            );
        };

        script.compile();
        return script;
    }

    private static String require(Map<String, String> values, String key) {
        String value = values.get(key);

        if (value == null) {
            throw new IllegalArgumentException("missing key %s".formatted(key));
        }

        return value;
    }

    private static void checkPresence(Map<String, String> values, String key, boolean expected, ScriptType type) {
        if (expected && !values.containsKey(key)) {
            throw new IllegalArgumentException("missing key %s, which %s scripts need".formatted(key, type));
        }

        if (!expected && values.containsKey(key)) {
            throw new IllegalArgumentException("key %s doesn't apply to %s scripts".formatted(key, type));
        }
    }

    private static int parseInt(Map<String, String> values, String key, int min, int max) {
        int value;

        try {
            value = Integer.parseInt(values.get(key));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("%s must be an integer, got '%s'".formatted(key, values.get(key)));
        }

        if (value < min || value > max) {
            throw new IllegalArgumentException("%s must be between %s and %s, got %s".formatted(key, min, max, value));
        }

        return value;
    }

    private static boolean parseBoolean(Map<String, String> values, String key) {
        return switch (values.get(key)) {
            case "true" -> true;
            case "false" -> false;
            default -> throw new IllegalArgumentException("%s must be true or false, got '%s'".formatted(key, values.get(key)));
        };
    }
}
