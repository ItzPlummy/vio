package com.plummy.vio.section;

public interface SectionVisitor<T> {

    T visitTextSection(TextSection section);
}
