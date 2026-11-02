package com.plummy.vio.client.hud;

import com.plummy.vio.client.FrameRenderer;
import com.plummy.vio.client.Theme;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

public final class StarRenderer {

    private static final long SPAWN_MILLIS = 100;
    private static final long LIFE_MILLIS = 2000;

    // Share of the icon's size a star falls over its life.
    private static final float FALL = 0.5f;

    private StarRenderer() {
    }

    // Takes the whole frame; stars spawn in its upper half and show only inside it.
    public static void render(GuiGraphics graphics, int x, int y, int size) {
        int[][] spans = FrameRenderer.getSpans(size, size, size / 2, FrameRenderer.BORDER);
        int color = Theme.ICON_STAR;
        long now = Util.getMillis();

        // Each star follows from its spawn time alone, so nothing has to be kept between frames.
        for (long star = (now - LIFE_MILLIS) / SPAWN_MILLIS + 1; star <= now / SPAWN_MILLIS; star++) {
            float age = (float) (now - star * SPAWN_MILLIS) / LIFE_MILLIS;
            int hash = Mth.murmurHash3Mixer((int) star);

            int column = Math.floorMod(hash, size);
            int row = Math.floorMod(Mth.murmurHash3Mixer(hash), size / 2) + (int) (age * size * FALL);

            if (row >= size || spans[row] == null || column < spans[row][0] || column >= spans[row][1]) {
                continue;
            }

            int alpha = Math.round((float) Math.sin(age * Math.PI) * 255);

            if (alpha <= 0) {
                continue;
            }

            graphics.fill(x + column, y + row, x + column + 1, y + row + 1, alpha << 24 | color & 0xFFFFFF);
        }
    }
}
