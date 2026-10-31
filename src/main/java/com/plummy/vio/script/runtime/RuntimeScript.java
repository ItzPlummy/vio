package com.plummy.vio.script.runtime;

import com.plummy.vio.script.ScriptLog;
import groovy.lang.Script;
import net.minecraft.server.MinecraftServer;

public abstract class RuntimeScript extends Script {

    private MinecraftServer server;
    private ScriptLog log;

    public void init(MinecraftServer server, ScriptLog log) {
        this.server = server;
        this.log = log;
    }

    public MinecraftServer getServer() {
        return server;
    }

    @Override
    public void println() {
        log.info("");
    }

    @Override
    public void println(Object value) {
        log.info(String.valueOf(value));
    }

    @Override
    public void print(Object value) {
        log.info(String.valueOf(value));
    }

    @Override
    public void printf(String format, Object value) {
        log.info(format.formatted(value));
    }

    @Override
    public void printf(String format, Object[] values) {
        log.info(format.formatted(values));
    }
}
