package com.plummy.vio.client;

import net.minecraft.client.gui.GuiGraphics;

// A dark take on the vanilla slot, with pixel-stepped rounded corners:
// a dark outline, then a 1px bevel that is light on the top-left and dark on the bottom-right, then the body.
public final class VioFrame {

    // Width of the outline plus the bevel: the body starts this far in.
    public static final int BORDER = 2;

    private VioFrame() {
    }

    public static void render(GuiGraphics graphics, int x, int y, int width, int height, int radius) {
        int[][] outline = spans(width, height, radius, 0);
        int[][] bevel = spans(width, height, radius, 1);
        int[][] body = spans(width, height, radius, 2);

        for (int row = 0; row < height; row++) {
            fillRing(graphics, x, y, width, height, row, outline[row], bevel[row], VioTheme.BORDER_DARK, VioTheme.BORDER_DARK);
            fillRing(graphics, x, y, width, height, row, bevel[row], body[row], VioTheme.BORDER_LIGHT, VioTheme.BORDER_SHADOW);

            if (body[row] != null) {
                graphics.fill(x + body[row][0], y + row, x + body[row][1], y + row + 1, VioTheme.BACKGROUND);
            }
        }
    }

    // Fills the pixels of a row that are inside `outer` but not inside `inner`, merging runs of the same color.
    // Pixels above the anti-diagonal of the frame get the top-left color, the rest the bottom-right one.
    private static void fillRing(GuiGraphics graphics, int x, int y, int width, int height, int row, int[] outer, int[] inner, int topLeftColor, int bottomRightColor) {
        if (outer == null) {
            return;
        }

        int runStart = -1;
        int runColor = 0;

        for (int column = outer[0]; column <= outer[1]; column++) {
            boolean inRing = column < outer[1] && (inner == null || column < inner[0] || column >= inner[1]);
            int color = (float) column / width + (float) row / height < 1 ? topLeftColor : bottomRightColor;

            if (runStart >= 0 && (!inRing || color != runColor)) {
                graphics.fill(x + runStart, y + row, x + column, y + row + 1, runColor);
                runStart = -1;
            }

            if (inRing && runStart < 0) {
                runStart = column;
                runColor = color;
            }
        }
    }

    // Per row, the [left, right) columns covered by the rounded rectangle inset by `inset`, or null for rows outside it.
    private static int[][] spans(int width, int height, int radius, int inset) {
        int[][] spans = new int[height][];
        int innerHeight = height - inset * 2;
        int cornerRadius = Math.max(0, Math.min(radius, Math.min(width, height) / 2) - inset);

        for (int row = 0; row < innerHeight; row++) {
            int distance = Math.min(row, innerHeight - 1 - row);
            int indent = 0;

            if (distance < cornerRadius) {
                float dy = cornerRadius - distance - 0.5f;
                indent = Math.round(cornerRadius - (float) Math.sqrt(cornerRadius * cornerRadius - dy * dy));
            }

            spans[inset + row] = new int[]{inset + indent, width - inset - indent};
        }

        return spans;
    }
}
