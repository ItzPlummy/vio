package com.plummy.vio.script;

import com.plummy.vio.config.ScriptLimits;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class DefaultScriptLog implements ScriptLog {

    private final int capacity;

    private final Deque<ScriptLogEntry> entries = new ArrayDeque<>();

    public DefaultScriptLog(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public synchronized List<ScriptLogEntry> getEntries(int limit, int offset) {
        return entries.stream().skip(offset).limit(limit).toList();
    }

    @Override
    public synchronized void log(String message, ScriptLogLevel level) {
        String text = message.length() > ScriptLimits.MAX_LOG_MESSAGE_LENGTH ? "%s ...".formatted(message.substring(0, ScriptLimits.MAX_LOG_MESSAGE_LENGTH)) : message;

        entries.addFirst(new ScriptLogEntry(text, Instant.now().truncatedTo(ChronoUnit.MILLIS), level));

        while (entries.size() > getCapacity()) {
            entries.removeLast();
        }
    }

    public int getCapacity() {
        return capacity;
    }
}
