package com.plummy.vio.script;

import java.util.List;

public interface ScriptLog {

    List<ScriptLogEntry> getEntries(int limit, int offset);

    void log(String message, ScriptLogLevel level);

    default List<ScriptLogEntry> getEntries() {
        return getEntries(Integer.MAX_VALUE, 0);
    }

    default List<ScriptLogEntry> getEntries(ScriptLogLevel level) {
        return getEntries().stream().filter(entry -> entry.level().isAtLeast(level)).toList();
    }

    default List<ScriptLogEntry> getEntries(int limit, int offset, ScriptLogLevel level) {
        return getEntries(level).stream().skip(offset).limit(limit).toList();
    }

    default void info(String message) {
        log(message, ScriptLogLevel.INFO);
    }

    default void warning(String message) {
        log(message, ScriptLogLevel.WARNING);
    }

    default void error(String message) {
        log(message, ScriptLogLevel.ERROR);
    }
}
