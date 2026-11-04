package com.plummy.vio.core.script;

public sealed interface Script permits RunnableScript, ToggleableScript {

    ScriptType getType();

    String getName();

    String getDescription();

    String getSource();

    <V> V accept(ScriptVisitor<V> visitor);
}
