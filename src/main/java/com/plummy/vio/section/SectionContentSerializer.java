package com.plummy.vio.section;

public class SectionContentSerializer implements SectionVisitor<String> {

    @Override
    public String visitTextSection(TextSection section) {
        return section.getText();
    }
}
