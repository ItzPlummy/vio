package com.plummy.vio.script;

public interface ScriptVisitor<T> {

    T visitOneShotScript(OneShotScript script);

    T visitPeriodicScript(PeriodicScript script);

    T visitCommandScript(CommandScript script);

    T visitListenerScript(ListenerScript script);
}
