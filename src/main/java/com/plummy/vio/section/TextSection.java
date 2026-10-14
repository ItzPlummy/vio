package com.plummy.vio.section;

public class TextSection implements Section {

    private final String text;

    public TextSection(String text) {
        this.text = text;
    }

    @Override
    public SectionType getType() {
        return SectionType.TEXT;
    }

    @Override
    public <T> T accept(SectionVisitor<T> visitor) {
        return visitor.visitTextSection(this);
    }

    public String getText() {
        return text;
    }
}
