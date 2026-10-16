package com.plummy.vio.client;

import net.minecraft.client.gui.GuiGraphics;

// A filled box with a 1px border, its corners stepped in by two pixels so it reads as rounded.
// Drawn in the current pose's units: in text space, the border is one font pixel thick, like the glyph strokes.
public final class VioBox {

    private VioBox() {
    }

    public static void render(GuiGraphics graphics, int x, int y, int width, int height, int fillColor, int borderColor) {
        int right = x + width;
        int bottom = y + height;

        // Body: a cross of two rectangles, which leaves the corners for the border.
        graphics.fill(x + 2, y + 1, right - 2, bottom - 1, fillColor);
        graphics.fill(x + 1, y + 2, right - 1, bottom - 2, fillColor);

        // Straight edges.
        graphics.fill(x + 2, y, right - 2, y + 1, borderColor);
        graphics.fill(x + 2, bottom - 1, right - 2, bottom, borderColor);
        graphics.fill(x, y + 2, x + 1, bottom - 2, borderColor);
        graphics.fill(right - 1, y + 2, right, bottom - 2, borderColor);

        // Corner steps.
        graphics.fill(x + 1, y + 1, x + 2, y + 2, borderColor);
        graphics.fill(right - 2, y + 1, right - 1, y + 2, borderColor);
        graphics.fill(x + 1, bottom - 2, x + 2, bottom - 1, borderColor);
        graphics.fill(right - 2, bottom - 2, right - 1, bottom - 1, borderColor);
    }
}
