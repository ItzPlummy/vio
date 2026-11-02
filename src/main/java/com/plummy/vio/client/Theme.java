package com.plummy.vio.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

public final class Theme {

    public static final int BACKGROUND = 0xFF1E1E22;
    public static final int BORDER_DARK = 0xFF0E0E10;
    public static final int BORDER_LIGHT = 0xFF3A3A42;
    public static final int BORDER_SHADOW = 0xFF141417;

    public static final int PLAYER_BOX = 0xFF34343A;
    public static final int PLAYER_BOX_BORDER = 0xFF4C4C55;
    public static final int PLAYER_TEXT = 0xFFFFFFFF;
    public static final int VIO_BOX = 0xFF2A1D3D;
    private static final int VIO_BOX_BORDER = 0xFF4A3270;
    public static final int ERROR_BOX = 0xFF2E1519;
    public static final int ERROR_BOX_BORDER = 0xFF5C2630;
    public static final int ERROR_TEXT = 0xFFFF8E99;
    public static final int INPUT_BOX = 0xFF151518;
    public static final int INPUT_BORDER = 0xFF34343A;
    private static final int INPUT_BORDER_FOCUSED = 0xFF5A3D8A;
    private static final int INPUT_SELECTION = 0x806A3FB5;
    private static final int VIO_TEXT = 0xFFC9A8FF;
    public static final int STATUS_TEXT = 0xFF8A8A92;
    public static final int CODE_TEXT = 0xFFE8E0F5;
    public static final int CODE_BACKGROUND = 0xFF1C1429;
    public static final int CODE_BLOCK_BORDER = 0xFF3B2A57;
    public static final int LINK_TEXT = 0xFF9FC4FF;
    public static final int HEADING_TEXT = 0xFFE9DCFF;
    public static final int STATUS_HIGHLIGHT = 0xFFE6E6EC;

    public static final int CUBE_FRONT = 0xFFA571F0;
    public static final int CUBE_BACK = 0xFF563185;
    public static final int CUBE_ACCENT = 0xFFC45DE3;

    public static final int ICON_OUTLINE = 0xFF0E0E10;
    private static final int ICON_LIGHT = 0xFF3E2A5C;
    public static final int ICON_SHADOW = 0xFF141417;
    public static final int ICON_BACKGROUND = 0xFF1E1E22;
    public static final int ICON_PULSE = 0xFF322640;
    public static final int ICON_STAR = 0xFF462E52;

    private static final float TEXT_SIZE = 2f / 3f;
    private static final float STATUS_TEXT_SIZE = 0.5f;

    private static final float HUE_SHIFT = 0.02f;
    private static final long VIO_TEXT_PERIOD_MILLIS = 5000;
    private static final long VIO_BOX_BORDER_PERIOD_MILLIS = 5300;
    private static final long INPUT_BORDER_FOCUSED_PERIOD_MILLIS = 4700;
    private static final long INPUT_SELECTION_PERIOD_MILLIS = 5600;
    private static final long ICON_LIGHT_PERIOD_MILLIS = 5150;

    private Theme() {
    }

    public static float getTextScale() {
        return getPixelScale(TEXT_SIZE);
    }

    public static float getStatusTextScale() {
        return getPixelScale(STATUS_TEXT_SIZE);
    }

    public static int getVioText() {
        return shiftHue(VIO_TEXT, VIO_TEXT_PERIOD_MILLIS);
    }

    public static int getVioBoxBorder() {
        return shiftHue(VIO_BOX_BORDER, VIO_BOX_BORDER_PERIOD_MILLIS);
    }

    public static int getInputBorderFocused() {
        return shiftHue(INPUT_BORDER_FOCUSED, INPUT_BORDER_FOCUSED_PERIOD_MILLIS);
    }

    public static int getInputSelection() {
        return shiftHue(INPUT_SELECTION, INPUT_SELECTION_PERIOD_MILLIS);
    }

    public static int getIconLight() {
        return shiftHue(ICON_LIGHT, ICON_LIGHT_PERIOD_MILLIS);
    }

    public static int lerp(int from, int to, float t) {
        if (t <= 0) {
            return from;
        }

        if (t >= 1) {
            return to;
        }

        int a = lerpChannel(from >>> 24, to >>> 24, t);
        int r = lerpChannel(from >> 16 & 0xFF, to >> 16 & 0xFF, t);
        int g = lerpChannel(from >> 8 & 0xFF, to >> 8 & 0xFF, t);
        int b = lerpChannel(from & 0xFF, to & 0xFF, t);

        return a << 24 | r << 16 | g << 8 | b;
    }

    private static int shiftHue(int color, long periodMillis) {
        double time = Util.getMillis() % periodMillis * Math.PI * 2 / periodMillis;
        float shift = (float) (-Math.cos(time) + 1) * HUE_SHIFT;

        float r = (color >> 16 & 0xFF) / 255f;
        float g = (color >> 8 & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float max = Math.max(r, Math.max(g, b));
        float delta = max - Math.min(r, Math.min(g, b));

        if (delta == 0) {
            return color;
        }

        float hue;

        if (max == r) {
            hue = (g - b) / delta / 6;
        } else if (max == g) {
            hue = ((b - r) / delta + 2) / 6;
        } else {
            hue = ((r - g) / delta + 4) / 6;
        }

        return Mth.hsvToArgb(Mth.frac(hue + shift), delta / max, max, color >>> 24);
    }

    private static float getPixelScale(float size) {
        double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
        long realPixels = Math.max(1, (long) Math.ceil(size * guiScale - 0.5));

        return (float) (realPixels / guiScale);
    }

    private static int lerpChannel(int from, int to, float t) {
        return Math.round(from + (to - from) * t);
    }
}
