package com.plummy.vio.core.script;

import java.util.LinkedHashMap;
import java.util.SequencedMap;

public class ScriptSerializer implements ScriptVisitor<String> {

    static final String HEADER_START = "/* @vio";
    static final String HEADER_END = "*/";

    private static final ScriptSerializer INSTANCE = new ScriptSerializer();

    public static ScriptSerializer getInstance() {
        return INSTANCE;
    }

    private ScriptSerializer() {
    }

    @Override
    public String visitOneShotScript(OneShotScript script) {
        String description = script.getDescription().replaceAll("\\R", " ");

        if (description.contains(HEADER_END)) {
            throw new IllegalArgumentException("A script description can't contain %s".formatted(HEADER_END));
        }

        SequencedMap<String, String> values = new LinkedHashMap<>();

        values.put("type", script.getType().getValue());
        values.put("description", description);
        values.put("creator", script.getCreator().toString());
        values.put("created_at", script.getCreatedAt().toString());

        return serialize(script, values);
    }

    @Override
    public String visitPeriodicScript(PeriodicScript script) {
        String description = script.getDescription().replaceAll("\\R", " ");

        if (description.contains(HEADER_END)) {
            throw new IllegalArgumentException("A script description can't contain %s".formatted(HEADER_END));
        }

        SequencedMap<String, String> values = new LinkedHashMap<>();

        values.put("type", script.getType().getValue());
        values.put("description", description);
        values.put("creator", script.getCreator().toString());
        values.put("created_at", script.getCreatedAt().toString());
        values.put("interval_ticks", Integer.toString(script.getIntervalTicks()));
        values.put("enabled", Boolean.toString(script.isEnabled()));
        values.put("toggled_by", script.getToggledBy().toString());
        values.put("toggled_at", script.getToggledAt().toString());

        return serialize(script, values);
    }

    @Override
    public String visitListenerScript(ListenerScript script) {
        String description = script.getDescription().replaceAll("\\R", " ");

        if (description.contains(HEADER_END)) {
            throw new IllegalArgumentException("A script description can't contain %s".formatted(HEADER_END));
        }

        SequencedMap<String, String> values = new LinkedHashMap<>();

        values.put("type", script.getType().getValue());
        values.put("description", description);
        values.put("creator", script.getCreator().toString());
        values.put("created_at", script.getCreatedAt().toString());
        values.put("enabled", Boolean.toString(script.isEnabled()));
        values.put("toggled_by", script.getToggledBy().toString());
        values.put("toggled_at", script.getToggledAt().toString());

        return serialize(script, values);
    }

    @Override
    public String visitCommandScript(CommandScript script) {
        String description = script.getDescription().replaceAll("\\R", " ");

        if (description.contains(HEADER_END)) {
            throw new IllegalArgumentException("A script description can't contain %s".formatted(HEADER_END));
        }

        SequencedMap<String, String> values = new LinkedHashMap<>();

        values.put("type", script.getType().getValue());
        values.put("description", description);
        values.put("creator", script.getCreator().toString());
        values.put("created_at", script.getCreatedAt().toString());
        values.put("permission_level", Integer.toString(script.getPermissionLevel()));
        values.put("enabled", Boolean.toString(script.isEnabled()));
        values.put("toggled_by", script.getToggledBy().toString());
        values.put("toggled_at", script.getToggledAt().toString());

        return serialize(script, values);
    }

    private static String serialize(Script script, SequencedMap<String, String> values) {
        StringBuilder text = new StringBuilder();

        text.append(HEADER_START).append('\n');
        values.forEach((key, value) -> text.append(key).append(": ").append(value).append('\n'));
        text.append(HEADER_END).append('\n');
        text.append(script.getSource());

        return text.toString();
    }
}
