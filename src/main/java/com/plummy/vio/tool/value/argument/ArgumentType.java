package com.plummy.vio.tool.value.argument;

public enum ArgumentType {

    INTEGER("integer"),
    FLOAT("float"),
    STRING("string"),
    BOOLEAN("boolean"),
    POSITION("position"),
    BLOCK_POSITION("block_position"),
    DIMENSION("dimension"),
    IDENTIFIER("identifier"),
    ENTITY_REFERENCE("entity_reference"),
    PLAYER_REFERENCE("player_reference"),
    ITEM_SPEC("item_spec"),
    BLOCK_SPEC("block_spec"),
    BLOCK_PREDICATE("block_predicate"),
    PAGE("page");

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

        throw new IllegalArgumentException("Invalid argument type: %s".formatted(value));
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
