package com.plummy.vio.client.screen;

import com.plummy.vio.client.ChatEntry;
import com.plummy.vio.client.ClientChatHistory;
import com.plummy.vio.client.TextLine;
import com.plummy.vio.client.VioBox;
import com.plummy.vio.client.VioTheme;
import com.plummy.vio.client.markdown.InlineMarkdown;
import com.plummy.vio.client.markdown.MarkdownLayout;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

// The scrollable conversation: player messages on the right, Vio's on the left, statuses small and gray.
// Player messages and Vio's replies carry a small gray postfix under the box: the time, and for replies how long Vio thought.
public class MessageList {

    private static final int LINE_HEIGHT = MarkdownLayout.LINE_HEIGHT;
    private static final int BOX_PADDING_X = 4;
    private static final int BOX_PADDING_Y = 3;
    private static final int GAP = 4;
    private static final float MAX_BOX_WIDTH = 0.75f;
    // Space between a box and its postfix.
    private static final int POSTFIX_GAP = 2;
    private static final int SCROLLBAR_GUTTER = 4;
    private static final float SCROLL_EASING = 14f;
    // The scrollbar fades in and out as the messages start or stop overflowing, e.g. while the window resizes.
    private static final float SCROLLBAR_FADE_EASING = 8f;
    private static final long SHIMMER_PERIOD_MILLIS = 1500;
    // Half the width of the highlight band, in text pixels.
    private static final float SHIMMER_BAND_WIDTH = 24f;
    private static final float MAX_FRAME_SECONDS = 0.1f;

    // Message times are UTC; the player sees them in their own time zone.
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

    private final List<Block> blocks = new ArrayList<>();

    private int layoutRevision = -1;
    private int layoutWidth = -1;
    private int contentHeight;
    // The last player message and everything after it, in text pixels.
    private int latestExchangeHeight;

    // Pixels scrolled up from the newest message; 0 means stuck to the bottom.
    private int scroll;
    // What is drawn: eases toward `scroll`, so the messages and the scrollbar glide instead of jumping.
    private float animatedScroll;
    // Keeps the latest player message at the top of the view, so a long reply grows downward
    // and its end stays hidden until the player scrolls down to it. Any manual scroll turns it off.
    private boolean anchored;
    private long lastRenderNanos = Util.getNanos();
    private int viewHeight;
    // Where the view was last drawn, in screen pixels, for finding what is under the mouse.
    private int viewX;
    private int viewY;
    private int viewWidth;
    private int viewScreenHeight;
    private int viewOverdraw;
    // 0-1, eases toward 1 while the messages overflow and toward 0 while they fit.
    private float scrollbarOpacity;

    public boolean overflows() {
        return contentHeight > viewHeight;
    }

    // Screen pixels scrolled up from the newest message.
    public int getScreenScroll() {
        return Math.round(scroll * VioTheme.textScale());
    }

    // Screen pixels of messages above the view, if the view were `viewHeight` screen pixels tall.
    public int getHiddenAbove(int viewHeight) {
        return Math.max(0, (int) Math.ceil(contentHeight * VioTheme.textScale()) - viewHeight - getScreenScroll());
    }

    // Amount in screen pixels.
    public void scrollBy(int amount) {
        anchored = false;
        scroll = clampScroll(scroll + Math.round(amount / VioTheme.textScale()));
    }

    public void anchorToLatestExchange() {
        anchored = true;
    }

    // Takes over the scroll state of the window being replaced, so reopening it does not move the view.
    public void continueFrom(MessageList other) {
        scroll = other.scroll;
        animatedScroll = other.animatedScroll;
        anchored = other.anchored;
        scrollbarOpacity = other.scrollbarOpacity;
    }

    // Height in screen pixels of the last player message and everything after it, for a width in screen pixels.
    public int getLatestExchangeHeight(Font font, int width) {
        layout(font, toContentWidth(width));
        return (int) Math.ceil(latestExchangeHeight * VioTheme.textScale());
    }

    // Position and size in screen pixels. Inside, everything is laid out in text space and drawn scaled down by
    // VioTheme.textScale(). Text space units are whole framebuffer pixels, so anything placed on them stays crisp.
    // Scrolling and anchoring use the view alone; older messages keep being drawn up to `overdraw` screen pixels
    // above it, where the caller fades them out.
    public void render(GuiGraphics graphics, Font font, int x, int y, int width, int height, int overdraw) {
        float scale = VioTheme.textScale();
        int contentWidth = toContentWidth(width);

        layout(font, contentWidth);
        viewHeight = (int) (height / scale);
        viewX = x;
        viewY = y;
        viewWidth = width;
        viewScreenHeight = height;
        viewOverdraw = overdraw;
        scroll = anchored ? Math.max(0, latestExchangeHeight - viewHeight) : clampScroll(scroll);
        animateScroll();

        // The scissor ignores the pose, so it stays in screen pixels.
        graphics.enableScissor(x, y - overdraw, x + width, y + height);
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(scale, scale, 1);

        // Bottom-aligned, like chat: short conversations sit right above the input.
        int blockY = viewHeight - contentHeight + Math.round(animatedScroll);
        int overdrawTop = -(int) Math.ceil(overdraw / scale);

        for (Block block : blocks) {
            if (blockY + block.height() >= overdrawTop && blockY < viewHeight) {
                renderBlock(graphics, font, block, 0, blockY, contentWidth);
            }

            blockY += block.height() + GAP;
        }

        graphics.pose().popPose();
        graphics.disableScissor();

        renderScrollbar(graphics, x + width - 2, y, height);
    }

    // The text style under a point in screen pixels, as last drawn, for clicking links in Vio's messages.
    // Null when the point is not over text.
    public Style getStyleAt(Font font, double mouseX, double mouseY) {
        if (mouseX < viewX || mouseX >= viewX + viewWidth || mouseY < viewY - viewOverdraw || mouseY >= viewY + viewScreenHeight) {
            return null;
        }

        float scale = VioTheme.textScale();
        double textX = (mouseX - viewX) / scale - BOX_PADDING_X;
        double textY = (mouseY - viewY) / scale;
        int blockY = viewHeight - contentHeight + Math.round(animatedScroll);

        for (Block block : blocks) {
            if (textY >= blockY && textY < blockY + block.boxHeight()) {
                if (block.kind() != ChatEntry.Kind.VIO) {
                    return null;
                }

                for (TextLine line : block.lines()) {
                    double lineY = textY - blockY - BOX_PADDING_Y - line.y();
                    double lineX = textX - line.x();

                    if (lineY >= 0 && lineY < LINE_HEIGHT && lineX >= 0 && lineX < font.width(line.text())) {
                        return font.getSplitter().componentStyleAtWidth(line.text(), (int) Math.floor(lineX));
                    }
                }

                return null;
            }

            blockY += block.height() + GAP;
        }

        return null;
    }

    private void layout(Font font, int width) {
        ClientChatHistory history = ClientChatHistory.getInstance();

        if (history.getRevision() == layoutRevision && width == layoutWidth) {
            return;
        }

        int previousHeight = contentHeight;
        int textWidth = (int) (width * MAX_BOX_WIDTH) - BOX_PADDING_X * 2;

        blocks.clear();
        contentHeight = 0;
        latestExchangeHeight = 0;

        // Statuses stay in the history but are not listed: only the latest one is shown, live, in the place
        // where the reply will appear, and the reply replaces it. Before the first one, a placeholder stands in.
        for (ChatEntry entry : history.getEntries()) {
            if (entry.getKind() != ChatEntry.Kind.STATUS) {
                addBlock(createBlock(font, entry, width, textWidth));
            }
        }

        ChatEntry last = history.getLastEntry();

        if (history.isBusy()) {
            ChatEntry status = last != null && last.getKind() == ChatEntry.Kind.STATUS
                    ? last
                    : new ChatEntry(ChatEntry.Kind.STATUS, history.getThinkingStatus(), Instant.now());

            addBlock(createBlock(font, status, width, textWidth));
        }

        // When reading older messages, new ones should not move the view.
        if (scroll > 0 && !anchored && width == layoutWidth) {
            scroll += contentHeight - previousHeight;
            animatedScroll += contentHeight - previousHeight;
        }

        layoutRevision = history.getRevision();
        layoutWidth = width;
    }

    private Block createBlock(Font font, ChatEntry entry, int width, int textWidth) {
        return switch (entry.getKind()) {
            case PLAYER, VIO, ERROR -> {
                // Vio writes markdown; the player's text and errors are shown as typed.
                MarkdownLayout.Layout layout = entry.getKind() == ChatEntry.Kind.VIO
                        ? MarkdownLayout.layout(font, entry.getText(), textWidth)
                        : new MarkdownLayout.Layout(plainLines(font, entry.getText(), textWidth), List.of());
                List<TextLine> lines = layout.lines();
                List<MarkdownLayout.Panel> panels = layout.panels();
                int textWidthUsed = Math.max(
                        lines.stream().mapToInt(line -> line.x() + font.width(line.text())).max().orElse(0),
                        panels.stream().mapToInt(panel -> panel.x() + panel.width()).max().orElse(0)
                );
                // Text rows end with an empty pixel row that the bottom padding replaces; a panel has none.
                int textHeight = Math.max(
                        lines.stream().mapToInt(TextLine::y).max().orElse(0) + LINE_HEIGHT,
                        panels.stream().mapToInt(panel -> panel.y() + panel.height() + 1).max().orElse(0)
                );
                int boxWidth = textWidthUsed + BOX_PADDING_X * 2;
                int boxHeight = textHeight - 1 + BOX_PADDING_Y * 2;
                String postfix = getPostfix(entry);
                int height = postfix == null ? boxHeight : boxHeight + POSTFIX_GAP + getPostfixHeight();

                yield new Block(entry.getKind(), lines, panels, boxWidth, boxHeight, postfix, height);
            }
            case STATUS -> {
                List<TextLine> lines = plainLines(font, entry.getText(), (int) (textWidth / statusScale()));
                int textLength = lines.stream().mapToInt(line -> font.width(line.text())).max().orElse(0);
                int height = (int) Math.ceil(lines.size() * LINE_HEIGHT * statusScale());

                yield new Block(entry.getKind(), lines, List.of(), textLength, height, null, height);
            }
        };
    }

    // Wrapped text without markdown, one row under the other.
    private static List<TextLine> plainLines(Font font, String text, int width) {
        List<TextLine> lines = new ArrayList<>();

        for (FormattedCharSequence line : font.split(FormattedText.of(text), width)) {
            lines.add(new TextLine(line, 0, lines.size() * LINE_HEIGHT));
        }

        return lines;
    }

    private static String getPostfix(ChatEntry entry) {
        String time = TIME_FORMAT.format(entry.getTime());

        return switch (entry.getKind()) {
            case PLAYER -> time;
            case VIO -> entry.getThought() == null ? time : time + " | " + getThoughtText(entry.getThought());
            default -> null;
        };
    }

    // Whole seconds, rounded up, so any reply took at least one.
    private static String getThoughtText(Duration thought) {
        long seconds = Math.max(1, (long) Math.ceil(thought.toMillis() / 1000.0));

        return seconds == 1 ? I18n.get("vio.chat.thought.one") : I18n.get("vio.chat.thought", seconds);
    }

    // In text space. Postfixes are drawn at the status size, and the font's last row is always empty, so it is left out.
    private static int getPostfixHeight() {
        return (int) Math.ceil((LINE_HEIGHT - 1) * statusScale());
    }

    private void addBlock(Block block) {
        contentHeight += (blocks.isEmpty() ? 0 : GAP) + block.height();
        latestExchangeHeight = block.kind() == ChatEntry.Kind.PLAYER || blocks.isEmpty()
                ? block.height()
                : latestExchangeHeight + GAP + block.height();
        blocks.add(block);
    }

    private void renderBlock(GuiGraphics graphics, Font font, Block block, int x, int y, int width) {
        switch (block.kind()) {
            case PLAYER -> {
                renderBox(graphics, font, block, x + width - block.width(), y, VioTheme.PLAYER_BOX, VioTheme.PLAYER_BOX_BORDER, VioTheme.PLAYER_TEXT);
                renderPostfix(graphics, font, block, x + width - BOX_PADDING_X, y, true);
            }
            case VIO -> {
                renderBox(graphics, font, block, x, y, VioTheme.VIO_BOX, VioTheme.VIO_BOX_BORDER, VioTheme.VIO_TEXT);
                renderPostfix(graphics, font, block, x + BOX_PADDING_X, y, false);
            }
            case STATUS -> {
                float statusScale = statusScale();

                graphics.pose().pushPose();
                graphics.pose().translate(x + BOX_PADDING_X, y, 0);
                graphics.pose().scale(statusScale, statusScale, 1);

                for (TextLine line : block.lines()) {
                    renderShimmer(graphics, font, line.text(), line.y(), block.width());
                }

                graphics.pose().popPose();
            }
            case ERROR -> renderBox(graphics, font, block, x, y, VioTheme.ERROR_BOX, VioTheme.ERROR_BOX_BORDER, VioTheme.ERROR_TEXT);
        }
    }

    // Draws the status one character at a time, lighting up the characters under a soft band that sweeps
    // from left to right over the text, then starts again.
    private static void renderShimmer(GuiGraphics graphics, Font font, FormattedCharSequence line, int y, int textLength) {
        float phase = (Util.getMillis() % SHIMMER_PERIOD_MILLIS) / (float) SHIMMER_PERIOD_MILLIS;
        float bandCenter = -SHIMMER_BAND_WIDTH + phase * (textLength + SHIMMER_BAND_WIDTH * 2);
        int[] x = {0};

        line.accept((index, style, codePoint) -> {
            String character = Character.toString(codePoint);
            int characterWidth = font.width(character);
            float distance = Math.abs(x[0] + characterWidth / 2f - bandCenter) / SHIMMER_BAND_WIDTH;
            float highlight = distance >= 1 ? 0 : (float) (Math.cos(distance * Math.PI) + 1) / 2;

            graphics.drawString(font, character, x[0], y, VioTheme.lerp(VioTheme.STATUS_TEXT, VioTheme.STATUS_HIGHLIGHT, highlight), false);
            x[0] += characterWidth;
            return true;
        });
    }

    // The border is a lighter shade of the box, so it stands out from the window without a new color.
    private static void renderBox(GuiGraphics graphics, Font font, Block block, int x, int y, int boxColor, int borderColor, int textColor) {
        VioBox.render(graphics, x, y, block.width(), block.boxHeight(), boxColor, borderColor);

        for (MarkdownLayout.Panel panel : block.panels()) {
            VioBox.render(graphics, x + BOX_PADDING_X + panel.x(), y + BOX_PADDING_Y + panel.y(), panel.width(), panel.height(), VioTheme.CODE_BACKGROUND, VioTheme.CODE_BLOCK_BORDER);
        }

        for (TextLine line : block.lines()) {
            int lineX = x + BOX_PADDING_X + line.x();
            int lineY = y + BOX_PADDING_Y + line.y();

            if (block.kind() == ChatEntry.Kind.VIO) {
                renderCodeBackgrounds(graphics, font, line.text(), lineX, lineY);
            }

            graphics.drawString(font, line.text(), lineX, lineY, textColor, true);
        }
    }

    // A darker patch behind each inline code run of a line, one pixel wider on each side than the glyphs.
    private static void renderCodeBackgrounds(GuiGraphics graphics, Font font, FormattedCharSequence line, int x, int y) {
        int[] position = {0};
        int[] runStart = {-1};

        line.accept((index, style, codePoint) -> {
            boolean code = InlineMarkdown.isCode(style);

            if (code && runStart[0] < 0) {
                runStart[0] = position[0];
            } else if (!code && runStart[0] >= 0) {
                fillCode(graphics, x + runStart[0], x + position[0], y);
                runStart[0] = -1;
            }

            position[0] += font.width(FormattedCharSequence.codepoint(codePoint, style));
            return true;
        });

        if (runStart[0] >= 0) {
            fillCode(graphics, x + runStart[0], x + position[0], y);
        }
    }

    // Glyph advances include a 1px gap on the right, so starting one pixel early centers the glyphs in the patch.
    // From a pixel above the glyphs down to their shadow, so patches on consecutive lines do not touch.
    private static void fillCode(GuiGraphics graphics, int left, int right, int y) {
        graphics.fill(left - 1, y - 1, right, y + LINE_HEIGHT - 1, VioTheme.CODE_BACKGROUND);
    }

    // Under the box, lined up with its text: `x` is where the postfix starts, or where it ends when aligned right.
    private static void renderPostfix(GuiGraphics graphics, Font font, Block block, int x, int y, boolean alignRight) {
        if (block.postfix() == null) {
            return;
        }

        float scale = statusScale();
        // Whole text pixels, so the postfix starts on a framebuffer pixel; aligned right, it may end up to one short.
        int left = alignRight ? x - (int) Math.ceil(font.width(block.postfix()) * scale) : x;

        graphics.pose().pushPose();
        graphics.pose().translate(left, y + block.boxHeight() + POSTFIX_GAP, 0);
        graphics.pose().scale(scale, scale, 1);
        graphics.drawString(font, block.postfix(), 0, 0, VioTheme.STATUS_TEXT, false);
        graphics.pose().popPose();
    }

    private void renderScrollbar(GuiGraphics graphics, int x, int y, int height) {
        int alpha = Math.round(scrollbarOpacity * 255);

        if (alpha <= 0) {
            return;
        }

        // Once everything fits, the thumb fills the whole track while it fades out.
        int maxScroll = contentHeight - viewHeight;
        int thumbHeight = maxScroll <= 0 ? height : Math.max(8, height * viewHeight / contentHeight);
        float scrolled = Math.max(0, Math.min(animatedScroll, maxScroll));
        int thumbY = maxScroll <= 0 ? y : y + Math.round((height - thumbHeight) * (maxScroll - scrolled) / maxScroll);

        graphics.fill(x, thumbY, x + 1, thumbY + thumbHeight, alpha << 24 | VioTheme.VIO_TEXT & 0xFFFFFF);
    }

    private void animateScroll() {
        long nanos = Util.getNanos();
        float seconds = Math.min((nanos - lastRenderNanos) / 1.0E9f, MAX_FRAME_SECONDS);
        lastRenderNanos = nanos;

        animatedScroll += (scroll - animatedScroll) * (1 - (float) Math.exp(-SCROLL_EASING * seconds));

        // Snap the last fraction of a pixel, so the view comes fully to rest.
        if (Math.abs(scroll - animatedScroll) < 0.5f) {
            animatedScroll = scroll;
        }

        float targetOpacity = overflows() ? 1 : 0;
        scrollbarOpacity += (targetOpacity - scrollbarOpacity) * (1 - (float) Math.exp(-SCROLLBAR_FADE_EASING * seconds));
    }

    // Status text size relative to chat text, since statuses are drawn inside the already scaled text space.
    private static float statusScale() {
        return VioTheme.statusTextScale() / VioTheme.textScale();
    }

    private static int toContentWidth(int width) {
        return (int) ((width - SCROLLBAR_GUTTER) / VioTheme.textScale());
    }

    private int clampScroll(int value) {
        return Math.max(0, Math.min(value, contentHeight - viewHeight));
    }

    // `boxHeight` is the box (or the status text) alone; `height` adds the postfix under it, if any.
    private record Block(ChatEntry.Kind kind, List<TextLine> lines, List<MarkdownLayout.Panel> panels, int width, int boxHeight, String postfix, int height) {
    }
}
