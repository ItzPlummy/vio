package com.plummy.vio.client;

import net.minecraft.client.gui.GuiGraphics;

public final class FrameRenderer {

    public static final int BORDER = 2;

    private FrameRenderer() {
    }

    public static void render(GuiGraphics graphics, int x, int y, int width, int height, int radius) {
        int[][] outline = getRoundedRectangleSpan(width, height, radius, 0);
        int[][] bevel = getRoundedRectangleSpan(width, height, radius, 1);
        int[][] body = getRoundedRectangleSpan(width, height, radius, 2);

        for (int row = 0; row < height; row++) {
            renderRing(graphics, x, y, width, height, row, outline[row], bevel[row], Theme.BORDER_DARK, Theme.BORDER_DARK);
            renderRing(graphics, x, y, width, height, row, bevel[row], body[row], Theme.BORDER_LIGHT, Theme.BORDER_SHADOW);

            if (body[row] != null) {
                graphics.fill(x + body[row][0], y + row, x + body[row][1], y + row + 1, Theme.BACKGROUND);
            }
        }
    }

    private static void renderRing(GuiGraphics graphics, int x, int y, int width, int height, int row, int[] outer, int[] inner, int topLeftColor, int bottomRightColor) {
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

    private static int[][] getRoundedRectangleSpan(int width, int height, int radius, int inset) {
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
