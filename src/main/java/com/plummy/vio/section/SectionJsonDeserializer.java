package com.plummy.vio.section;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.plummy.vio.access.AccessLevel;

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
            case PLAYER_CONTEXT -> new PlayerContextSection(
                    UUID.fromString(json.get("id").getAsString()),
                    json.get("name").getAsString(),
                    AccessLevel.fromValue(json.get("access_level").getAsString()),
                    json.get("x").getAsDouble(),
                    json.get("y").getAsDouble(),
                    json.get("z").getAsDouble(),
                    json.get("yaw").getAsDouble(),
                    json.get("pitch").getAsDouble()
            );
            case SERVER_CONTEXT -> {
                List<String> dimensions = new ArrayList<>();

                for (JsonElement dimension : json.getAsJsonArray("dimensions")) {
                    dimensions.add(dimension.getAsString());
                }

                yield new ServerContextSection(dimensions, json.get("time").getAsLong());
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
