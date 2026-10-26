package com.plummy.vio.core.script;

public interface ScriptVisitor<T> {

    T visitOneShotScript(OneShotScript script);

    T visitPeriodicScript(PeriodicScript script);

    T visitListenerScript(ListenerScript script);

    T visitCommandScript(CommandScript script);
}
