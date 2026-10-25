package com.plummy.vio.core.script.dsl;

import com.plummy.vio.core.script.ScriptLog;
import groovy.lang.Script;
import net.minecraft.server.MinecraftServer;

// Base class of every script: what a script can use besides the server's own API. Output goes to the script's
// log instead of the console.
public abstract class VioScript extends Script {

    private MinecraftServer server;
    private ScriptLog log;

    // Called by Vio before the script body runs.
    public void init(MinecraftServer server, ScriptLog log) {
        this.server = server;
        this.log = log;
    }

    public MinecraftServer getServer() {
        return server;
    }

    public ScriptLog getLog() {
        return log;
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
        log.info(String.format(format, value));
    }

    @Override
    public void printf(String format, Object[] values) {
        log.info(String.format(format, values));
    }
}
