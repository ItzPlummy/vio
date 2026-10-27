package com.plummy.vio.section;

public interface Section {

    SectionType getType();

    <T> T accept(SectionVisitor<T> visitor);
}
