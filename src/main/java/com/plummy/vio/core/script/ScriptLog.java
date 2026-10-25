package com.plummy.vio.core.script;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

// The latest output and errors of one script, kept in memory only. Synchronized, since a script may log from
// threads of its own.
public class ScriptLog {

    private static final int CAPACITY = 200;
    private static final int MAX_MESSAGE_LENGTH = 1000;

    private final Deque<Entry> entries = new ArrayDeque<>();
    // Every entry ever added, so callers can tell which entries arrived after a point.
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
        String text = message.length() > MAX_MESSAGE_LENGTH ? message.substring(0, MAX_MESSAGE_LENGTH) + " ..." : message;

        if (entries.size() == CAPACITY) {
            entries.removeFirst();
        }

        entries.addLast(new Entry(Instant.now(), level, text));
        added++;
    }

    // Oldest first.
    public synchronized List<Entry> getEntries() {
        return List.copyOf(entries);
    }

    // A point to pass to getEntriesSince.
    public synchronized long mark() {
        return added;
    }

    // Entries added after the mark that are still kept, oldest first.
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
