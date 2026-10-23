package com.plummy.vio.client;

import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FormattedCharSequence;

public final class ShimmerRenderer {

    private static final long PERIOD_MILLIS = 1500;
    private static final float BAND_WIDTH = 24f;

    private ShimmerRenderer() {
    }

    // A light band sweeps across the text, in font pixels from x 0.
    public static void render(GuiGraphics graphics, Font font, FormattedCharSequence line, int y, int textLength) {
        float phase = (Util.getMillis() % PERIOD_MILLIS) / (float) PERIOD_MILLIS;
        float bandCenter = -BAND_WIDTH + phase * (textLength + BAND_WIDTH * 2);
        int[] x = {0};

        line.accept((index, style, codePoint) -> {
            String character = Character.toString(codePoint);
            int characterWidth = font.width(character);
            float distance = Math.abs(x[0] + characterWidth / 2f - bandCenter) / BAND_WIDTH;
            float highlight = distance >= 1 ? 0 : (float) (Math.cos(distance * Math.PI) + 1) / 2;

            graphics.drawString(font, character, x[0], y, Theme.lerp(Theme.STATUS_TEXT, Theme.STATUS_HIGHLIGHT, highlight), false);
            x[0] += characterWidth;
            return true;
        });
    }
}
