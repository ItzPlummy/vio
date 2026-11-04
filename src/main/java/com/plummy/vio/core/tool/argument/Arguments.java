package com.plummy.vio.core.tool.argument;

import com.google.gson.*;

public class Arguments {

    private final JsonObject json;

    public Arguments(JsonObject json) {
        this.json = json.deepCopy();
    }

    public static Arguments fromString(String string) {
        JsonElement element;

        try {
            element = JsonParser.parseString(string);
        } catch (JsonParseException e) {
            throw new UnprocessableException("Arguments are not valid JSON: %s".formatted(string));
        }

        if (!element.isJsonObject()) {
            throw new UnprocessableException("Arguments must be a JSON object, got: %s".formatted(string));
        }

        return new Arguments(element.getAsJsonObject());
    }

    public int getInt(String name) {
        JsonPrimitive primitive = getPrimitive(name);

        if (!primitive.isNumber() || primitive.getAsDouble() != Math.rint(primitive.getAsDouble())) {
            throw new UnprocessableException("%s must be an integer, got %s".formatted(name, primitive));
        }

        return primitive.getAsInt();
    }

    public int getInt(String name, int min, int max) {
        int value = getInt(name);

        if (value < min || value > max) {
            throw new UnprocessableException("%s must be between %s and %s, got %s".formatted(name, min, max, value));
        }

        return value;
    }

    public float getFloat(String name) {
        JsonPrimitive primitive = getPrimitive(name);

        if (!primitive.isNumber()) {
            throw new UnprocessableException("%s must be a number, got %s".formatted(name, primitive));
        }

        return primitive.getAsFloat();
    }

    public float getFloat(String name, float min, float max) {
        float value = getFloat(name);

        if (value < min || value > max) {
            throw new UnprocessableException("%s must be between %s and %s, got %s".formatted(name, min, max, value));
        }

        return value;
    }

    public String getString(String name) {
        JsonPrimitive primitive = getPrimitive(name);

        if (!primitive.isString()) {
            throw new UnprocessableException("%s must be a string, got %s".formatted(name, primitive));
        }

        return primitive.getAsString();
    }

    public boolean getBoolean(String name) {
        JsonPrimitive primitive = getPrimitive(name);

        if (!primitive.isBoolean()) {
            throw new UnprocessableException("%s must be a boolean, got %s".formatted(name, primitive));
        }

        return primitive.getAsBoolean();
    }

    private JsonElement getElement(String name) {
        JsonElement element = json.get(name);

        if (element == null || element.isJsonNull()) {
            throw new UnprocessableException("Missing required argument %s".formatted(name));
        }

        return element;
    }

    private JsonPrimitive getPrimitive(String name) {
        JsonElement element = getElement(name);

        if (!element.isJsonPrimitive()) {
            throw new UnprocessableException("%s must be a primitive, got %s".formatted(name, element));
        }

        return element.getAsJsonPrimitive();
    }
}
