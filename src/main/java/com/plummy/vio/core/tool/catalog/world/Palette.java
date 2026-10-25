package com.plummy.vio.core.tool.catalog.world;

import java.util.*;

class Palette {

    private static final List<Character> KEYS = new ArrayList<>();

    static {
        for (char c = 'a'; c <= 'z'; c++) {
            KEYS.add(c);
        }

        for (char c = 'A'; c <= 'Z'; c++) {
            KEYS.add(c);
        }

        for (char c = '0'; c <= '9'; c++) {
            KEYS.add(c);
        }

        for (char c = '!'; c <= '~'; c++) {
            if (!Character.isLetterOrDigit(c) && c != '.' && c != '?' && c != '"' && c != '\\') {
                KEYS.add(c);
            }
        }
    }

    private final Map<String, Character> keys = new LinkedHashMap<>();

    Character getKey(String entry) {
        Character key = keys.get(entry);

        if (key == null) {
            key = pick(entry);

            if (key != null) {
                keys.put(entry, key);
            }
        }

        return key;
    }

    Map<String, Character> getKeys() {
        return Collections.unmodifiableMap(keys);
    }

    private Character pick(String entry) {
        Collection<Character> used = keys.values();
        String path = entry.substring(entry.indexOf(':') + 1);

        if (!path.isEmpty()) {
            char preferred = path.charAt(0);

            if (KEYS.contains(preferred) && !used.contains(preferred)) {
                return preferred;
            }
        }

        for (char key : KEYS) {
            if (!used.contains(key)) {
                return key;
            }
        }

        return null;
    }
}
