package com.plummy.vio.core.section;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.plummy.vio.core.access.AccessLevel;
import com.plummy.vio.core.script.ScriptState;
import com.plummy.vio.core.script.ScriptType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SectionJsonDeserializer {

    private static final SectionJsonDeserializer INSTANCE = new SectionJsonDeserializer();

    public static SectionJsonDeserializer getInstance() {
        return INSTANCE;
    }

    private SectionJsonDeserializer() {
    }

    public Section deserialize(JsonObject json) {
        return switch (SectionType.fromValue(json.get("type").getAsString())) {
            case TEXT -> new TextSection(
                    json.get("text").getAsString()
            );
            case ACTIVITY_DESCRIPTION -> new ActivityDescriptionSection(
                    json.get("activity_description").getAsString()
            );
            case PLAYER_REQUEST -> new PlayerRequestSection(
                    json.get("request").getAsString()
            );
            case PLAYER_CONTEXT -> {
                List<String> structures = new ArrayList<>();

                for (JsonElement structure : json.getAsJsonArray("structures")) {
                    structures.add(structure.getAsString());
                }

                yield new PlayerContextSection(
                        UUID.fromString(json.get("id").getAsString()),
                        json.get("name").getAsString(),
                        AccessLevel.fromValue(json.get("access_level").getAsString()),
                        json.get("x").getAsDouble(),
                        json.get("y").getAsDouble(),
                        json.get("z").getAsDouble(),
                        json.get("yaw").getAsDouble(),
                        json.get("pitch").getAsDouble(),
                        json.get("dimension").getAsString(),
                        json.get("biome").getAsString(),
                        structures.toArray(new String[0]),
                        json.get("health").getAsDouble(),
                        json.get("armor").getAsDouble(),
                        json.get("food").getAsDouble(),
                        json.get("saturation").getAsDouble(),
                        json.get("level").getAsInt(),
                        json.get("game_mode").getAsString(),
                        json.get("main_hand_item").getAsString(),
                        json.get("off_hand_item").getAsString()
                );
            }
            case SERVER_CONTEXT -> {
                List<String> dimensions = new ArrayList<>();

                for (JsonElement dimension : json.getAsJsonArray("dimensions")) {
                    dimensions.add(dimension.getAsString());
                }

                yield new ServerContextSection(
                        dimensions.toArray(new String[0]),
                        json.get("time").getAsLong(),
                        json.get("day").getAsLong()
                );
            }
            case SCRIPT_CONTEXT -> {
                List<ScriptContextSection.Entry> scripts = new ArrayList<>();

                for (JsonElement element : json.getAsJsonArray("scripts")) {
                    JsonObject script = element.getAsJsonObject();

                    scripts.add(new ScriptContextSection.Entry(
                            script.get("name").getAsString(),
                            ScriptType.fromValue(script.get("type").getAsString()),
                            ScriptState.fromValue(script.get("state").getAsString()),
                            script.get("description").getAsString()
                    ));
                }

                yield new ScriptContextSection(scripts);
            }
        };
    }

    public <T extends Section> T deserialize(JsonObject json, Class<T> expectedClass) {
        Section section = deserialize(json);

        if (!expectedClass.isInstance(section)) {
            throw new JsonParseException("Expected " + expectedClass.getSimpleName() + ", got " + section.getType());
        }

        return expectedClass.cast(section);
    }
}
