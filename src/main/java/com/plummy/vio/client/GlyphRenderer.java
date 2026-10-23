package com.plummy.vio.client;

import net.minecraft.client.gui.GuiGraphics;

// Small pixel icons drawn in font pixels, so under the same scale as text they line up with it and stay as crisp.
public final class GlyphRenderer {

    // As tall as the font's capital letters.
    public static final String[] MICROPHONE = {
            ".###.",
            ".###.",
            ".###.",
            "#.#.#",
            ".###.",
            "..#..",
            ".###."
    };

    public static final String[] CROSS = {
            ".....",
            "#...#",
            ".#.#.",
            "..#..",
            ".#.#.",
            "#...#",
            "....."
    };

    private GlyphRenderer() {
    }

    public static int getWidth(String[] glyph) {
        return glyph[0].length();
    }

    public static void render(GuiGraphics graphics, String[] glyph, int x, int y, int color) {
        for (int row = 0; row < glyph.length; row++) {
            for (int column = 0; column < glyph[row].length(); column++) {
                if (glyph[row].charAt(column) == '#') {
                    graphics.fill(x + column, y + row, x + column + 1, y + row + 1, color);
                }
            }
        }
    }
}
