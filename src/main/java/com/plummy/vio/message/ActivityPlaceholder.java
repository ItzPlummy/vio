package com.plummy.vio.message;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ActivityPlaceholder {

    private static final List<String> PLACEHOLDERS = List.of(
            "Thinking",
            "Tinkering",
            "Brewing"
    );

    public static String getRandom() {
        return PLACEHOLDERS.get(ThreadLocalRandom.current().nextInt(PLACEHOLDERS.size())) + "...";
    }
}
