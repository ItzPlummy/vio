package com.plummy.vio.tool;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

public class ToolArguments {

    private final JsonObject arguments;

    public ToolArguments(JsonObject arguments) {
        this.arguments = arguments;
    }

    public static ToolArguments parse(String raw) {
        JsonElement element;

        try {
            element = JsonParser.parseString(raw);
        } catch (JsonParseException e) {
            throw new ToolArgumentException("Arguments are not valid JSON: " + raw);
        }

        if (!element.isJsonObject()) {
            throw new ToolArgumentException("Arguments must be a JSON object, got: " + raw);
        }

        return new ToolArguments(element.getAsJsonObject());
    }

    public JsonObject getArguments() {
        return arguments;
    }

    public int getInt(String name) {
        JsonPrimitive primitive = getPrimitive(name);

        if (!primitive.isNumber() || primitive.getAsDouble() != Math.rint(primitive.getAsDouble())) {
            throw new ToolArgumentException("'" + name + "' must be an integer, got " + primitive);
        }

        return primitive.getAsInt();
    }

    public int getInt(String name, int min, int max) {
        int value = getInt(name);

        if (value < min || value > max) {
            throw new ToolArgumentException("'" + name + "' must be between " + min + " and " + max + ", got " + value);
        }

        return value;
    }

    public float getFloat(String name) {
        JsonPrimitive primitive = getPrimitive(name);

        if (!primitive.isNumber()) {
            throw new ToolArgumentException("'" + name + "' must be a number, got " + primitive);
        }

        return primitive.getAsFloat();
    }

    public float getFloat(String name, float min, float max) {
        float value = getFloat(name);

        if (value < min || value > max) {
            throw new ToolArgumentException("'" + name + "' must be between " + min + " and " + max + ", got " + value);
        }

        return value;
    }

    public String getString(String name) {
        JsonPrimitive primitive = getPrimitive(name);

        if (!primitive.isString()) {
            throw new ToolArgumentException("'" + name + "' must be a string, got " + primitive);
        }

        return primitive.getAsString();
    }

    public boolean getBoolean(String name) {
        JsonPrimitive primitive = getPrimitive(name);

        if (!primitive.isBoolean()) {
            throw new ToolArgumentException("'" + name + "' must be a boolean, got " + primitive);
        }

        return primitive.getAsBoolean();
    }

    private JsonPrimitive getPrimitive(String name) {
        JsonElement element = arguments.get(name);

        if (element == null || element.isJsonNull()) {
            throw new ToolArgumentException("Missing required argument '" + name + "'");
        }

        if (!element.isJsonPrimitive()) {
            throw new ToolArgumentException("'" + name + "' must be a single value, got " + element);
        }

        return element.getAsJsonPrimitive();
    }
}
