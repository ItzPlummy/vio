package com.plummy.vio.tool;

import com.google.gson.*;
import com.plummy.vio.access.AccessLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.SequencedMap;

public interface Tool {

    String getName();

    String getDescription();

    SequencedMap<String, ArgumentDefinition> getArgumentSchema();

    AccessLevel getAccessLevel();

    ExecutionResult execute(Context context, Arguments arguments);

    class Context {

        private final ServerPlayer player;

        public Context(ServerPlayer player) {
            this.player = player;
        }

        public ServerPlayer getPlayer() {
            return player;
        }

        public MinecraftServer getServer() {
            return player.getServer();
        }

        public ServerLevel getLevel() {
            return player.serverLevel();
        }
    }

    class Arguments {

        private final JsonObject arguments;

        public Arguments(JsonObject arguments) {
            this.arguments = arguments;
        }

        public static Arguments parse(String raw) {
            JsonElement element;

            try {
                element = JsonParser.parseString(raw);
            } catch (JsonParseException e) {
                throw new ArgumentException("Arguments are not valid JSON: " + raw);
            }

            if (!element.isJsonObject()) {
                throw new ArgumentException("Arguments must be a JSON object, got: " + raw);
            }

            return new Arguments(element.getAsJsonObject());
        }

        public JsonObject getArguments() {
            return arguments;
        }

        public int getInt(String name) {
            JsonPrimitive primitive = getPrimitive(name);

            if (!primitive.isNumber() || primitive.getAsDouble() != Math.rint(primitive.getAsDouble())) {
                throw new ArgumentException("'" + name + "' must be an integer, got " + primitive);
            }

            return primitive.getAsInt();
        }

        public int getInt(String name, int min, int max) {
            int value = getInt(name);

            if (value < min || value > max) {
                throw new ArgumentException("'" + name + "' must be between " + min + " and " + max + ", got " + value);
            }

            return value;
        }

        public float getFloat(String name) {
            JsonPrimitive primitive = getPrimitive(name);

            if (!primitive.isNumber()) {
                throw new ArgumentException("'" + name + "' must be a number, got " + primitive);
            }

            return primitive.getAsFloat();
        }

        public float getFloat(String name, float min, float max) {
            float value = getFloat(name);

            if (value < min || value > max) {
                throw new ArgumentException("'" + name + "' must be between " + min + " and " + max + ", got " + value);
            }

            return value;
        }

        public String getString(String name) {
            JsonPrimitive primitive = getPrimitive(name);

            if (!primitive.isString()) {
                throw new ArgumentException("'" + name + "' must be a string, got " + primitive);
            }

            return primitive.getAsString();
        }

        public boolean getBoolean(String name) {
            JsonPrimitive primitive = getPrimitive(name);

            if (!primitive.isBoolean()) {
                throw new ArgumentException("'" + name + "' must be a boolean, got " + primitive);
            }

            return primitive.getAsBoolean();
        }

        private JsonPrimitive getPrimitive(String name) {
            JsonElement element = arguments.get(name);

            if (element == null || element.isJsonNull()) {
                throw new ArgumentException("Missing required argument '" + name + "'");
            }

            if (!element.isJsonPrimitive()) {
                throw new ArgumentException("'" + name + "' must be a single value, got " + element);
            }

            return element.getAsJsonPrimitive();
        }
    }

    class ArgumentDefinition {

        private final ArgumentType type;
        private final String description;

        public ArgumentDefinition(ArgumentType type, String description) {
            this.type = type;
            this.description = description;
        }

        public ArgumentType getType() {
            return type;
        }

        public String getDescription() {
            return description;
        }
    }

    enum ArgumentType {

        INTEGER("integer"),
        FLOAT("number"),
        STRING("string"),
        BOOLEAN("boolean");

        private final String value;

        ArgumentType(String value) {
            this.value = value;
        }

        public static ArgumentType fromValue(String value) {
            for (ArgumentType type : values()) {
                if (type.getValue().equals(value)) {
                    return type;
                }
            }

            throw new IllegalArgumentException("Invalid tool argument type: " + value);
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    class ArgumentException extends RuntimeException {

        public ArgumentException(String message) {
            super(message);
        }
    }

    class ExecutionRequest {

        private final String name;
        private final Arguments arguments;

        public ExecutionRequest(String name, Arguments arguments) {
            this.name = name;
            this.arguments = arguments;
        }

        public String getName() {
            return name;
        }

        public Arguments getArguments() {
            return arguments;
        }
    }

    class ExecutionResult {

        private final ExecutionStatus status;
        private final String result;

        public ExecutionResult(ExecutionStatus status, String result) {
            this.status = status;
            this.result = result;
        }

        public static ExecutionResult success(String result) {
            return new ExecutionResult(ExecutionStatus.SUCCESS, result);
        }

        public static ExecutionResult partial(String result) {
            return new ExecutionResult(ExecutionStatus.PARTIAL, result);
        }

        public static ExecutionResult invalidArguments(String result) {
            return new ExecutionResult(ExecutionStatus.INVALID_ARGUMENTS, result);
        }

        public static ExecutionResult denied(String result) {
            return new ExecutionResult(ExecutionStatus.DENIED, result);
        }

        public static ExecutionResult failure(String result) {
            return new ExecutionResult(ExecutionStatus.FAILURE, result);
        }

        public ExecutionStatus getStatus() {
            return status;
        }

        public String getResult() {
            return result;
        }
    }

    enum ExecutionStatus {

        SUCCESS("success"),
        PARTIAL("partial"),
        INVALID_ARGUMENTS("invalid_arguments"),
        DENIED("denied"),
        FAILURE("failure");

        private final String value;

        ExecutionStatus(String value) {
            this.value = value;
        }

        public static ExecutionStatus fromValue(String value) {
            for (ExecutionStatus status : values()) {
                if (status.getValue().equals(value)) {
                    return status;
                }
            }

            throw new IllegalArgumentException("Invalid tool execution status: " + value);
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return value;
        }
    }
}
