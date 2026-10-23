package com.plummy.vio.tool.argument;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Map;
import java.util.SequencedMap;

public class ArgumentSchemaJsonSerializer {

    private static final ArgumentSchemaJsonSerializer INSTANCE = new ArgumentSchemaJsonSerializer();

    public static ArgumentSchemaJsonSerializer getInstance() {
        return INSTANCE;
    }

    private ArgumentSchemaJsonSerializer() {
    }

    // Strict-mode schema shared by all providers: every property required, no additional properties.
    public JsonObject serialize(SequencedMap<String, ArgumentDefinition> properties) {
        JsonObject propertiesJson = new JsonObject();
        JsonArray required = new JsonArray();

        for (Map.Entry<String, ArgumentDefinition> entry : properties.entrySet()) {
            propertiesJson.add(entry.getKey(), serialize(entry.getValue()));
            required.add(entry.getKey());
        }

        JsonObject json = new JsonObject();

        json.addProperty("type", "object");
        json.add("properties", propertiesJson);
        json.add("required", required);
        json.addProperty("additionalProperties", false);

        return json;
    }

    public JsonObject serialize(ArgumentDefinition definition) {
        JsonObject json = new JsonObject();

        switch (definition.getType()) {
            case INTEGER -> json.addProperty("type", "integer");
            case FLOAT -> json.addProperty("type", "number");
            case STRING, DIMENSION, IDENTIFIER, ENTITY_REFERENCE, PLAYER_REFERENCE, ITEM_SPEC, BLOCK_SPEC, BLOCK_PREDICATE -> json.addProperty("type", "string");
            case BOOLEAN -> json.addProperty("type", "boolean");
            case BLOCK_POSITION -> addCoordinates(json, "integer");
            case POSITION -> addCoordinates(json, "number");
        }

        json.addProperty("description", definition.getDescription());

        if (!definition.getEnumValues().isEmpty()) {
            JsonArray values = new JsonArray();
            definition.getEnumValues().forEach(values::add);
            json.add("enum", values);
        }

        return json;
    }

    // [x, y, z]
    private static void addCoordinates(JsonObject json, String coordinateType) {
        JsonObject items = new JsonObject();
        items.addProperty("type", coordinateType);

        json.addProperty("type", "array");
        json.add("items", items);
        json.addProperty("minItems", 3);
        json.addProperty("maxItems", 3);
    }
}
