package com.plummy.vio.script;

public sealed interface Executable extends Script permits OneShotScript, PeriodicScript {
}
