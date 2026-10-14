package com.plummy.vio.section;

public enum SectionType {

    TEXT("text");

    private final String value;

    SectionType(String value) {
        this.value = value;
    }

    public static SectionType fromValue(String value) {
        for (SectionType type : SectionType.values()) {
            if (type.getValue().equals(value)) {
                return type;
            }
        }

        throw new IllegalArgumentException("Invalid section type: " + value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
