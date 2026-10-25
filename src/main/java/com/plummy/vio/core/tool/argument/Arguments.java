package com.plummy.vio.core.tool.argument;

import com.google.gson.*;

public class Arguments {

    private final JsonObject arguments;

    public Arguments(JsonObject arguments) {
        this.arguments = arguments;
    }

    public static Arguments parse(String raw) {
        JsonElement element;

        try {
            element = JsonParser.parseString(raw);
        } catch (JsonParseException e) {
            throw new Argument.ParseException("Arguments are not valid JSON: " + raw);
        }

        if (!element.isJsonObject()) {
            throw new Argument.ParseException("Arguments must be a JSON object, got: " + raw);
        }

        return new Arguments(element.getAsJsonObject());
    }

    public JsonObject getArguments() {
        return arguments;
    }

    public String getStatus() {
        JsonElement element = arguments.get("status");

        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            return "";
        }

        return element.getAsString().strip();
    }

    public int getInt(String name) {
        JsonPrimitive primitive = getPrimitive(name);

        if (!primitive.isNumber() || primitive.getAsDouble() != Math.rint(primitive.getAsDouble())) {
            throw new Argument.ParseException("'" + name + "' must be an integer, got " + primitive);
        }

        return primitive.getAsInt();
    }

    public int getInt(String name, int min, int max) {
        int value = getInt(name);

        if (value < min || value > max) {
            throw new Argument.ParseException("'" + name + "' must be between " + min + " and " + max + ", got " + value);
        }

        return value;
    }

    public float getFloat(String name) {
        JsonPrimitive primitive = getPrimitive(name);

        if (!primitive.isNumber()) {
            throw new Argument.ParseException("'" + name + "' must be a number, got " + primitive);
        }

        return primitive.getAsFloat();
    }

    public float getFloat(String name, float min, float max) {
        float value = getFloat(name);

        if (value < min || value > max) {
            throw new Argument.ParseException("'" + name + "' must be between " + min + " and " + max + ", got " + value);
        }

        return value;
    }

    public String getString(String name) {
        JsonPrimitive primitive = getPrimitive(name);

        if (!primitive.isString()) {
            throw new Argument.ParseException("'" + name + "' must be a string, got " + primitive);
        }

        return primitive.getAsString();
    }

    public boolean getBoolean(String name) {
        JsonPrimitive primitive = getPrimitive(name);

        if (!primitive.isBoolean()) {
            throw new Argument.ParseException("'" + name + "' must be a boolean, got " + primitive);
        }

        return primitive.getAsBoolean();
    }

    public <E extends Enum<E>> E getEnum(String name, Class<E> enumClass) {
        String value = getString(name);

        for (E constant : enumClass.getEnumConstants()) {
            if (ArgumentDefinition.getEnumValue(constant).equals(value)) {
                return constant;
            }
        }

        throw new Argument.ParseException("'" + name + "' must be one of " + ArgumentDefinition.getEnumValues(enumClass) + ", got \"" + value + "\"");
    }

    // Every argument is required, so an optional one is passed as "", meaning "none" or "any". Check this before reading a typed optional argument.
    public boolean isEmpty(String name) {
        return getString(name).isBlank();
    }

    // Blank comes back as null.
    public String getOptionalString(String name) {
        return isEmpty(name) ? null : getString(name);
    }

    public BlockPosition getBlockPosition(String name) {
        return ArgumentJsonDeserializer.getInstance().deserializeBlockPosition(name, getElement(name));
    }

    public Position getPosition(String name) {
        return ArgumentJsonDeserializer.getInstance().deserializePosition(name, getElement(name));
    }

    public Dimension getDimension(String name) {
        return ArgumentJsonDeserializer.getInstance().deserializeDimension(name, getElement(name));
    }

    public Identifier getIdentifier(String name) {
        return ArgumentJsonDeserializer.getInstance().deserializeIdentifier(name, getElement(name));
    }

    public EntityReference getEntityReference(String name) {
        return ArgumentJsonDeserializer.getInstance().deserializeEntityReference(name, getElement(name));
    }

    public PlayerReference getPlayerReference(String name) {
        return ArgumentJsonDeserializer.getInstance().deserializePlayerReference(name, getElement(name));
    }

    public ItemSpec getItemSpec(String name) {
        return ArgumentJsonDeserializer.getInstance().deserializeItemSpec(name, getElement(name));
    }

    public BlockSpec getBlockSpec(String name) {
        return ArgumentJsonDeserializer.getInstance().deserializeBlockSpec(name, getElement(name));
    }

    public BlockPredicate getBlockPredicate(String name) {
        return ArgumentJsonDeserializer.getInstance().deserializeBlockPredicate(name, getElement(name));
    }

    // Reads the "limit" and "offset" arguments every paged tool declares.
    public Page getPage(int maxLimit) {
        return new Page(getInt("limit", 1, maxLimit), getInt("offset", 0, Integer.MAX_VALUE));
    }

    private JsonElement getElement(String name) {
        JsonElement element = arguments.get(name);

        if (element == null || element.isJsonNull()) {
            throw new Argument.ParseException("Missing required argument '" + name + "'");
        }

        return element;
    }

    private JsonPrimitive getPrimitive(String name) {
        JsonElement element = getElement(name);

        if (!element.isJsonPrimitive()) {
            throw new Argument.ParseException("'" + name + "' must be a single value, got " + element);
        }

        return element.getAsJsonPrimitive();
    }
}
