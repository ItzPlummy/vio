package com.plummy.vio.message;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ActivityPlaceholder {

    private static final List<String> PLACEHOLDERS = List.of(
            "Thinking",
            "Tinkering",
            "Crafting",
            "Brewing",
            "Enchanting",
            "Smelting",
            "Mining for ideas",
            "Consulting the villagers"
    );

    public static String random() {
        return PLACEHOLDERS.get(ThreadLocalRandom.current().nextInt(PLACEHOLDERS.size())) + "...";
    }
}
