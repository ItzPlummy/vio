package com.plummy.vio.core.tool.value.argument;

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

    public static ArgumentDefinition status() {
        return new ArgumentDefinition(ArgumentType.STRING, "Status shown to the player while this tool runs, in the language of your replies: 2-5 words describing only this action, ending with '...', e.g. 'Checking inventory...'. No reasoning, explanations or results.");
    }

    // Read with Arguments.getPage and the same maxLimit.
    public static ArgumentDefinition page(int maxLimit, int defaultLimit, String noun) {
        return new ArgumentDefinition(ArgumentType.PAGE, "Which %s to return: limit is how many, 1 to %s (%s is a good default); offset is how many to skip, 0 for the first page.".formatted(noun, maxLimit, defaultLimit));
    }

    public static <E extends Enum<E>> ArgumentDefinition enumeration(Class<E> enumClass, String description) {
        return new ArgumentDefinition(ArgumentType.STRING, description, getEnumValues(enumClass));
    }

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
