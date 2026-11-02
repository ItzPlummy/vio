package com.plummy.vio.script;

public sealed interface Toggleable extends Script permits PeriodicScript, CommandScript, ListenerScript {

    boolean isEnabled();

    Toggleable withEnabled(boolean isEnabled);
}
