package com.plummy.vio.script;

public interface ScriptScheduler {

    void schedule(long delayTicks, Runnable task);
}
