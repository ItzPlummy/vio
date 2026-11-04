package com.plummy.vio.core.script;

public interface ScriptVisitor<T> {

    T visit(OneShotScript script);

    T visit(PeriodicScript script);

    T visit(CommandScript script);

    T visit(ListenerScript script);
}
