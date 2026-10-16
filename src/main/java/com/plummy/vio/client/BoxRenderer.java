package com.plummy.vio.client;

import net.minecraft.client.gui.GuiGraphics;

public final class BoxRenderer {

    private BoxRenderer() {
    }

    public static void render(GuiGraphics graphics, int x, int y, int width, int height, int fillColor, int borderColor) {
        int right = x + width;
        int bottom = y + height;

        graphics.fill(x + 2, y + 1, right - 2, bottom - 1, fillColor);
        graphics.fill(x + 1, y + 2, right - 1, bottom - 2, fillColor);

        graphics.fill(x + 2, y, right - 2, y + 1, borderColor);
        graphics.fill(x + 2, bottom - 1, right - 2, bottom, borderColor);
        graphics.fill(x, y + 2, x + 1, bottom - 2, borderColor);
        graphics.fill(right - 1, y + 2, right, bottom - 2, borderColor);

        graphics.fill(x + 1, y + 1, x + 2, y + 2, borderColor);
        graphics.fill(right - 2, y + 1, right - 1, y + 2, borderColor);
        graphics.fill(x + 1, bottom - 2, x + 2, bottom - 1, borderColor);
        graphics.fill(right - 2, bottom - 2, right - 1, bottom - 1, borderColor);
    }
}
