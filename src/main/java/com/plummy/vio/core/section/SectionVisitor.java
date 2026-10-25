package com.plummy.vio.core.section;

public interface SectionVisitor<T> {

    T visitTextSection(TextSection section);

    T visitActivityDescriptionSection(ActivityDescriptionSection section);

    T visitPlayerRequestSection(PlayerRequestSection section);

    T visitPlayerContextSection(PlayerContextSection section);

    T visitServerContextSection(ServerContextSection section);

    T visitScriptContextSection(ScriptContextSection section);
}
