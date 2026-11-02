package com.plummy.vio.script.runtime;

import com.plummy.vio.script.ScriptLog;
import com.plummy.vio.script.ScriptScheduler;
import com.plummy.vio.script.state.FileState;
import com.plummy.vio.script.state.MemoryState;
import groovy.lang.Script;
import net.minecraft.server.MinecraftServer;

public abstract class RuntimeScript extends Script {

    private MinecraftServer server;
    private ScriptLog log;
    private MemoryState memoryState;
    private FileState fileState;
    private ScriptScheduler scheduler;

    public void init(MinecraftServer server, ScriptLog log, MemoryState memoryState, FileState fileState, ScriptScheduler scheduler) {
        this.server = server;
        this.log = log;
        this.memoryState = memoryState;
        this.fileState = fileState;
        this.scheduler = scheduler;
    }

    public void after(long ticks, Runnable task) {
        scheduler.schedule(ticks, task);
    }

    public MinecraftServer getServer() {
        return server;
    }

    public MemoryState getMemoryState() {
        return memoryState;
    }

    public FileState getFileState() {
        return fileState;
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
