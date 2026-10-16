package com.plummy.vio.client.screen;

import com.plummy.vio.client.VioTheme;
import com.plummy.vio.network.payload.PlayerRequestPayload;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.MultilineTextField;

import java.util.ArrayList;
import java.util.List;

// Vanilla's wrapping text logic (cursor, selection, clipboard, word jumps) with Vio's own rendering.
public class InputField extends MultilineTextField {

    // The field works in text space (vanilla font pixels); it is drawn scaled down by `scale`.
    private static final int LINE_HEIGHT = 9;

    private static final int SELECTION = 0x806A3FB5;
    private static final long CARET_BLINK_MILLIS = 300;

    private final Font font;
    // Fixed for the field's lifetime, since the wrap width depends on it. The screen makes a new field on resize,
    // which is also what a GUI scale change triggers.
    private final float scale;
    // Rounded up to whole screen pixels. Each line starts on its own screen pixel row, so all of them stay crisp.
    private final int screenLineHeight;

    private int firstVisibleLine;
    private long lastEditMillis = Util.getMillis();

    // Width in screen pixels.
    public InputField(Font font, int width) {
        this(font, width, VioTheme.textScale());
    }

    private InputField(Font font, int width, float scale) {
        super(font, (int) (width / scale));
        this.font = font;
        this.scale = scale;
        // The small margin keeps float error (e.g. 9 * 2/3 = 6.0000005) from adding a pixel.
        this.screenLineHeight = (int) Math.ceil(LINE_HEIGHT * scale - 0.001f);
        setCharacterLimit(PlayerRequestPayload.MAX_LENGTH);
    }

    public float getScale() {
        return scale;
    }

    public int getScreenLineHeight() {
        return screenLineHeight;
    }

    // Keeps the caret solid while typing, like vanilla text boxes.
    public void onEdit() {
        lastEditMillis = Util.getMillis();
    }

    // Coordinates in screen pixels, relative to the field's top-left corner.
    public void clickAt(double x, double y) {
        int line = firstVisibleLine + (int) Math.floor(y / screenLineHeight);

        // Vanilla finds the line from the y in text space, so aim at the middle of the clicked one.
        seekCursorToPoint(x / scale, line * LINE_HEIGHT + LINE_HEIGHT / 2.0);
        onEdit();
    }

    // Position in screen pixels.
    public void render(GuiGraphics graphics, int screenX, int screenY, int visibleLines, String placeholder) {
        String value = value();

        if (value.isEmpty()) {
            beginLine(graphics, screenX, screenY);
            graphics.drawString(font, placeholder, 0, 0, VioTheme.STATUS_TEXT, false);
            graphics.pose().popPose();
        }

        List<StringView> lines = new ArrayList<>();
        iterateLines().forEach(lines::add);

        int cursorLine = Math.max(0, getLineAtCursor());

        // Scroll just enough to keep the caret's line visible.
        firstVisibleLine = Math.min(firstVisibleLine, cursorLine);
        firstVisibleLine = Math.max(firstVisibleLine, cursorLine - visibleLines + 1);
        firstVisibleLine = Math.max(0, Math.min(firstVisibleLine, lines.size() - visibleLines));

        StringView selection = getSelected();
        boolean caretVisible = (Util.getMillis() - lastEditMillis) / CARET_BLINK_MILLIS % 2 == 0;
        int lastLine = Math.min(lines.size(), firstVisibleLine + visibleLines);

        for (int index = firstVisibleLine; index < lastLine; index++) {
            StringView line = lines.get(index);

            beginLine(graphics, screenX, screenY + (index - firstVisibleLine) * screenLineHeight);

            int selectionStart = Math.max(selection.beginIndex(), line.beginIndex());
            int selectionEnd = Math.min(selection.endIndex(), line.endIndex());

            if (selectionStart < selectionEnd) {
                int left = font.width(value.substring(line.beginIndex(), selectionStart));
                int right = font.width(value.substring(line.beginIndex(), selectionEnd));

                graphics.fill(left, -1, right, LINE_HEIGHT, SELECTION);
            }

            graphics.drawString(font, value.substring(line.beginIndex(), line.endIndex()), 0, 0, VioTheme.PLAYER_TEXT, false);

            if (caretVisible && index == cursorLine) {
                int caretX = font.width(value.substring(line.beginIndex(), Math.min(cursor(), line.endIndex())));

                graphics.fill(caretX, -1, caretX + 1, LINE_HEIGHT, VioTheme.VIO_TEXT);
            }

            graphics.pose().popPose();
        }
    }

    // Pushes a pose with text space starting at the given screen position; the caller pops it.
    private void beginLine(GuiGraphics graphics, int screenX, int screenY) {
        graphics.pose().pushPose();
        graphics.pose().translate(screenX, screenY, 0);
        graphics.pose().scale(scale, scale, 1);
    }
}
