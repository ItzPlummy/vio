package com.plummy.vio.client;

import net.minecraft.client.Minecraft;

// All Vio UI colors, as ARGB.
public final class VioTheme {

    public static final int BACKGROUND = 0xFF1E1E22;
    public static final int BORDER_DARK = 0xFF0E0E10;
    public static final int BORDER_LIGHT = 0xFF3A3A42;
    public static final int BORDER_SHADOW = 0xFF141417;

    public static final int PLAYER_BOX = 0xFF34343A;
    public static final int PLAYER_BOX_BORDER = 0xFF4C4C55;
    public static final int PLAYER_TEXT = 0xFFFFFFFF;
    public static final int VIO_BOX = 0xFF2A1D3D;
    public static final int VIO_BOX_BORDER = 0xFF4A3270;
    // An error reads as a Vio message gone wrong: the same box, shifted to red. The text is a soft tint of ACCENT,
    // since the full-strength red is hard to read on a dark box.
    public static final int ERROR_BOX = 0xFF2E1519;
    public static final int ERROR_BOX_BORDER = 0xFF5C2630;
    public static final int ERROR_TEXT = 0xFFFF8E99;
    // Darker than the window, so the field looks inset. The border turns violet while the field takes typing.
    public static final int INPUT_BOX = 0xFF151518;
    public static final int INPUT_BORDER = 0xFF34343A;
    public static final int INPUT_BORDER_FOCUSED = 0xFF5A3D8A;
    public static final int VIO_TEXT = 0xFFC9A8FF;
    public static final int STATUS_TEXT = 0xFF8A8A92;
    // Markdown in Vio's messages: inline code on a darker patch of the Vio box, links in blue.
    public static final int CODE_TEXT = 0xFFE8E0F5;
    public static final int CODE_BACKGROUND = 0xFF1C1429;
    public static final int CODE_BLOCK_BORDER = 0xFF3B2A57;
    public static final int LINK_TEXT = 0xFF9FC4FF;
    public static final int HEADING_TEXT = 0xFFE9DCFF;
    public static final int STATUS_HIGHLIGHT = 0xFFE6E6EC;

    public static final int CUBE_FRONT = 0xFFB98CFF;
    public static final int CUBE_BACK = 0xFF4A2580;
    public static final int CUBE_ACCENT = 0xFFDA8DF0;

    // Preferred text sizes, as fractions of the vanilla font size. The drawn size is the nearest one at the
    // current GUI scale where a font pixel covers whole framebuffer pixels (see pixelScale).
    private static final float TEXT_SIZE = 2f / 3f;
    private static final float STATUS_TEXT_SIZE = 0.5f;

    private VioTheme() {
    }

    // Scale for chat text.
    public static float textScale() {
        return pixelScale(TEXT_SIZE);
    }

    // Scale for status lines, relative to the vanilla font like textScale, not to chat text.
    public static float statusTextScale() {
        return pixelScale(STATUS_TEXT_SIZE);
    }

    // The scale closest to `size` at which one font pixel is a whole number of framebuffer pixels (at least one),
    // so glyph strokes stay even and the 1px text shadow does not smear. Ties round down, to keep text from growing.
    private static float pixelScale(float size) {
        double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
        long realPixels = Math.max(1, (long) Math.ceil(size * guiScale - 0.5));

        return (float) (realPixels / guiScale);
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

    private static int lerpChannel(int from, int to, float t) {
        return Math.round(from + (to - from) * t);
    }
}
