package com.plummy.vio.core.tool.argument;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public class ArgumentDefinition {

    private final ArgumentType type;
    private final String description;
    private final List<String> enumValues;

    public ArgumentDefinition(ArgumentType type, String description) {
        this(type, description, List.of());
    }

    private ArgumentDefinition(ArgumentType type, String description, List<String> enumValues) {
        this.type = type;
        this.description = description;
        this.enumValues = List.copyOf(enumValues);
    }

    // Values are the lowercase constant names, read back with Arguments.getEnum.
    public static <E extends Enum<E>> ArgumentDefinition enumeration(Class<E> enumClass, String description) {
        return new ArgumentDefinition(ArgumentType.STRING, description, getEnumValues(enumClass));
    }

    // For enums where only some constants are valid input.
    public static <E extends Enum<E>> ArgumentDefinition enumeration(Collection<E> constants, String description) {
        return new ArgumentDefinition(ArgumentType.STRING, description, constants.stream().map(ArgumentDefinition::getEnumValue).toList());
    }

    public static <E extends Enum<E>> String getEnumValue(E constant) {
        return constant.name().toLowerCase(Locale.ROOT);
    }

    public static <E extends Enum<E>> List<String> getEnumValues(Class<E> enumClass) {
        return Arrays.stream(enumClass.getEnumConstants()).map(ArgumentDefinition::getEnumValue).toList();
    }

    public ArgumentType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getEnumValues() {
        return enumValues;
    }
}
