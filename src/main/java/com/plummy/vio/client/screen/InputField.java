package com.plummy.vio.client.screen;

import com.plummy.vio.client.Theme;
import com.plummy.vio.config.AgentLimits;
import com.plummy.vio.network.payload.PlayerRequestPayload;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.MultilineTextField;

import java.util.ArrayList;
import java.util.List;

public class InputField extends MultilineTextField {

    private static final int LINE_HEIGHT = 9;
    private static final long CARET_BLINK_MILLIS = 300;

    private final Font font;
    private final float scale;
    private final int screenLineHeight;

    private int firstVisibleLine;
    private long lastEditMillis = Util.getMillis();

    public InputField(Font font, int width) {
        this(font, width, Theme.getTextScale());
    }

    private InputField(Font font, int width, float scale) {
        super(font, (int) (width / scale));
        this.font = font;
        this.scale = scale;
        this.screenLineHeight = (int) Math.ceil(LINE_HEIGHT * scale - 0.001f);
        setCharacterLimit(AgentLimits.MAX_REQUEST_LENGTH);
    }

    public float getScale() {
        return scale;
    }

    public int getScreenLineHeight() {
        return screenLineHeight;
    }

    public void onEdit() {
        lastEditMillis = Util.getMillis();
    }

    public void clickAt(double x, double y) {
        int line = firstVisibleLine + (int) Math.floor(y / screenLineHeight);

        seekCursorToPoint(x / scale, line * LINE_HEIGHT + LINE_HEIGHT / 2.0);
        onEdit();
    }

    public void render(GuiGraphics graphics, int screenX, int screenY, int visibleLines, String placeholder) {
        String value = value();

        if (value.isEmpty()) {
            beginLine(graphics, screenX, screenY);
            graphics.drawString(font, placeholder, 0, 0, Theme.STATUS_TEXT, false);
            graphics.pose().popPose();
        }

        List<StringView> lines = new ArrayList<>();
        iterateLines().forEach(lines::add);

        int cursorLine = Math.max(0, getLineAtCursor());

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

                graphics.fill(left, -1, right, LINE_HEIGHT, Theme.INPUT_SELECTION);
            }

            graphics.drawString(font, value.substring(line.beginIndex(), line.endIndex()), 0, 0, Theme.PLAYER_TEXT, false);

            if (caretVisible && index == cursorLine) {
                int caretX = font.width(value.substring(line.beginIndex(), Math.min(cursor(), line.endIndex())));

                graphics.fill(caretX, -1, caretX + 1, LINE_HEIGHT, Theme.VIO_TEXT);
            }

            graphics.pose().popPose();
        }
    }

    private void beginLine(GuiGraphics graphics, int screenX, int screenY) {
        graphics.pose().pushPose();
        graphics.pose().translate(screenX, screenY, 0);
        graphics.pose().scale(scale, scale, 1);
    }
}
