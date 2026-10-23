package com.plummy.vio.tool.argument;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;
import java.util.UUID;

public class ArgumentJsonDeserializer {

    private static final ArgumentJsonDeserializer INSTANCE = new ArgumentJsonDeserializer();

    public static ArgumentJsonDeserializer getInstance() {
        return INSTANCE;
    }

    private ArgumentJsonDeserializer() {
    }

    public Position deserializePosition(String name, JsonElement json) {
        if (!(json instanceof JsonArray array) || array.size() != 3) {
            throw new Argument.ParseException("%s must be [x, y, z], got %s".formatted(name, json));
        }

        for (JsonElement element : array) {
            if (!(element instanceof JsonPrimitive primitive) || !primitive.isNumber()) {
                throw new Argument.ParseException("%s must contain only numbers, got %s".formatted(name, json));
            }
        }

        return new Position(array.get(0).getAsDouble(), array.get(1).getAsDouble(), array.get(2).getAsDouble());
    }

    public BlockPosition deserializeBlockPosition(String name, JsonElement json) {
        if (!(json instanceof JsonArray array) || array.size() != 3) {
            throw new Argument.ParseException("%s must be [x, y, z], got %s".formatted(name, json));
        }

        for (JsonElement element : array) {
            if (!(element instanceof JsonPrimitive primitive) || !primitive.isNumber() || primitive.getAsDouble() != Math.rint(primitive.getAsDouble())) {
                throw new Argument.ParseException("%s must contain only integers, got %s".formatted(name, json));
            }
        }

        return new BlockPosition(array.get(0).getAsInt(), array.get(1).getAsInt(), array.get(2).getAsInt());
    }

    public Dimension deserializeDimension(String name, JsonElement json) {
        return new Dimension(parseId(name, getString(name, json)));
    }

    public Identifier deserializeIdentifier(String name, JsonElement json) {
        String value = getString(name, json).strip();

        if (value.startsWith("#")) {
            return new Identifier(parseId(name, value.substring(1)), true);
        }

        return new Identifier(parseId(name, value), false);
    }

    // A UUID, otherwise a player name.
    public EntityReference deserializeEntityReference(String name, JsonElement json) {
        String value = getReferenceString(name, json);

        try {
            return new UuidReference(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            return new PlayerReference(value);
        }
    }

    public PlayerReference deserializePlayerReference(String name, JsonElement json) {
        return new PlayerReference(getReferenceString(name, json));
    }

    public ItemSpec deserializeItemSpec(String name, JsonElement json) {
        return new ItemSpec(getNonBlankString(name, json));
    }

    public BlockSpec deserializeBlockSpec(String name, JsonElement json) {
        return new BlockSpec(getNonBlankString(name, json));
    }

    public BlockPredicate deserializeBlockPredicate(String name, JsonElement json) {
        return new BlockPredicate(getNonBlankString(name, json));
    }

    // Selectors are never accepted: every entity a tool touches is named explicitly.
    private static String getReferenceString(String name, JsonElement json) {
        String value = getNonBlankString(name, json);

        if (value.startsWith("@")) {
            throw new Argument.ParseException("Selectors like \"%s\" are not accepted in %s; pass a UUID or a player name. Use find_entities to look up UUIDs.".formatted(value, name));
        }

        return value;
    }

    private static String getNonBlankString(String name, JsonElement json) {
        String value = getString(name, json).strip();

        if (value.isEmpty()) {
            throw new Argument.ParseException("%s must not be empty".formatted(name));
        }

        return value;
    }

    private static String getString(String name, JsonElement json) {
        if (!(json instanceof JsonPrimitive primitive) || !primitive.isString()) {
            throw new Argument.ParseException("%s must be a string, got %s".formatted(name, json));
        }

        return primitive.getAsString();
    }

    // Ids without a namespace are accepted like commands do: "stone" is "minecraft:stone".
    private static ResourceLocation parseId(String name, String value) {
        ResourceLocation id = ResourceLocation.tryParse(value.strip().toLowerCase(Locale.ROOT));

        if (id == null) {
            throw new Argument.ParseException("%s is not a valid id: \"%s\"".formatted(name, value));
        }

        return id;
    }
}
