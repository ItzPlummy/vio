package com.plummy.vio.section;

public interface SectionVisitor<T> {

    T visitTextSection(TextSection section);

    T visitActivityDescriptionSection(ActivityDescriptionSection section);

    T visitPlayerRequestSection(PlayerRequestSection section);

    T visitPlayerContextSection(PlayerContextSection section);

    T visitServerContextSection(ServerContextSection section);
}
