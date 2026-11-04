package com.plummy.vio.core.script;

public sealed interface ToggleableScript extends Script permits PeriodicScript, CommandScript, ListenerScript {

    boolean isEnabled();

    void enable();

    void disable();
}
