package com.plummy.vio.client;

import net.minecraft.client.resources.language.I18n;

import java.util.concurrent.ThreadLocalRandom;

// Random statuses shown while Vio thinks without a status of its own, e.g. before its first tool call.
// They live in the lang files as numbered keys (vio.status.placeholder.1, .2, ...), so every language
// and resource pack can have its own set; the list ends at the first missing number.
public final class StatusPlaceholders {

    private static final String KEY_PREFIX = "vio.status.placeholder.";
    private static final String FALLBACK = "...";

    private StatusPlaceholders() {
    }

    public static String random() {
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
