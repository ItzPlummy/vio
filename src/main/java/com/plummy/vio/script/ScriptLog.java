package com.plummy.vio.script;

import com.plummy.vio.config.ScriptLimits;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class ScriptLog {

    private final Deque<Entry> entries = new ArrayDeque<>();
    private long added;

    public void info(String message) {
        add(Level.INFO, message);
    }

    public void warn(String message) {
        add(Level.WARN, message);
    }

    public void error(String message) {
        add(Level.ERROR, message);
    }

    public synchronized void add(Level level, String message) {
        String text = message.length() > ScriptLimits.MAX_LOG_MESSAGE_LENGTH ? "%s ...".formatted(message.substring(0, ScriptLimits.MAX_LOG_MESSAGE_LENGTH)) : message;

        if (entries.size() == ScriptLimits.LOG_CAPACITY) {
            entries.removeFirst();
        }

        entries.addLast(new Entry(Instant.now(), level, text));
        added++;
    }

    public synchronized List<Entry> getEntries() {
        return List.copyOf(entries);
    }

    public synchronized long mark() {
        return added;
    }

    public synchronized List<Entry> getEntriesSince(long mark) {
        List<Entry> all = List.copyOf(entries);
        int count = (int) Math.min(all.size(), added - mark);

        return all.subList(all.size() - count, all.size());
    }

    public record Entry(Instant time, Level level, String message) {
    }

    public enum Level {

        INFO("info"),
        WARN("warn"),
        ERROR("error");

        private final String value;

        Level(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return value;
        }
    }
}
