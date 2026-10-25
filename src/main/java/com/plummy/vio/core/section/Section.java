package com.plummy.vio.core.section;

public interface Section {

    SectionType getType();

    <T> T accept(SectionVisitor<T> visitor);
}
