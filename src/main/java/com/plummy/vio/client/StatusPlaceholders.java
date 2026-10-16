package com.plummy.vio.client;

import net.minecraft.client.resources.language.I18n;

import java.util.concurrent.ThreadLocalRandom;

public final class StatusPlaceholders {

    private static final String KEY_PREFIX = "vio.status.placeholder.";
    private static final String FALLBACK = "...";

    private StatusPlaceholders() {
    }

    public static String getRandom() {
        int count = 0;

        while (I18n.exists(KEY_PREFIX + (count + 1))) {
            count++;
        }

        if (count == 0) {
            return FALLBACK;
        }

        return I18n.get(KEY_PREFIX + (ThreadLocalRandom.current().nextInt(count) + 1));
    }
}
