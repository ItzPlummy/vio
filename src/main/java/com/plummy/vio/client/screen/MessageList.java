package com.plummy.vio.client.screen;

import com.plummy.vio.client.BoxRenderer;
import com.plummy.vio.client.GlyphRenderer;
import com.plummy.vio.client.ShimmerRenderer;
import com.plummy.vio.client.TextLine;
import com.plummy.vio.client.Theme;
import com.plummy.vio.client.conversation.ClientConversation;
import com.plummy.vio.client.markdown.InlineMarkdown;
import com.plummy.vio.client.markdown.MarkdownLayout;
import com.plummy.vio.client.message.ClientMessage;
import com.plummy.vio.client.message.ClientMessageType;
import com.plummy.vio.client.message.PlayerMessage;
import com.plummy.vio.client.message.StatusMessage;
import com.plummy.vio.client.message.ThoughtMessage;
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

public class MessageList {

    private static final int LINE_HEIGHT = MarkdownLayout.LINE_HEIGHT;
    private static final int BOX_PADDING_X = 4;
    private static final int BOX_PADDING_Y = 3;
    private static final int GAP = 4;
    private static final float MAX_BOX_WIDTH = 0.75f;
    private static final int POSTFIX_GAP = 2;
    private static final int GLYPH_GAP = 2;
    private static final int SCROLLBAR_GUTTER = 4;
    private static final float SCROLL_EASING = 14f;
    private static final float SCROLLBAR_FADE_EASING = 8f;
    private static final float MAX_FRAME_SECONDS = 0.1f;

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

    private final List<Block> blocks = new ArrayList<>();

    private int layoutRevision = -1;
    private int layoutWidth = -1;
    private int contentHeight;
    private int latestExchangeHeight;

    private int scroll;
    private float animatedScroll;
    private boolean anchored;
    private long lastRenderNanos = Util.getNanos();
    private int viewHeight;
    private int viewX;
    private int viewY;
    private int viewWidth;
    private int viewScreenHeight;
    private int viewOverdraw;
    private float scrollbarOpacity;

    public boolean isOverflowing() {
        return contentHeight > viewHeight;
    }

    public int getScreenScroll() {
        return Math.round(scroll * Theme.getTextScale());
    }

    public int getHiddenAbove(int viewHeight) {
        return Math.max(0, (int) Math.ceil(contentHeight * Theme.getTextScale()) - viewHeight - getScreenScroll());
    }

    public int getLatestExchangeHeight(Font font, int width) {
        layout(font, getContentWidth(width));
        return (int) Math.ceil(latestExchangeHeight * Theme.getTextScale());
    }

    public void scrollBy(int amount) {
        anchored = false;
        scroll = clampScroll(scroll + Math.round(amount / Theme.getTextScale()));
    }

    public void anchorToLatestExchange() {
        anchored = true;
    }

    public void continueFrom(MessageList other) {
        scroll = other.scroll;
        animatedScroll = other.animatedScroll;
        anchored = other.anchored;
        scrollbarOpacity = other.scrollbarOpacity;
    }

    public void render(GuiGraphics graphics, Font font, int x, int y, int width, int height, int overdraw) {
        float scale = Theme.getTextScale();
        int contentWidth = getContentWidth(width);

        layout(font, contentWidth);
        viewHeight = (int) (height / scale);
        viewX = x;
        viewY = y;
        viewWidth = width;
        viewScreenHeight = height;
        viewOverdraw = overdraw;
        scroll = anchored ? Math.max(0, latestExchangeHeight - viewHeight) : clampScroll(scroll);
        animateScroll();

        graphics.enableScissor(x, y - overdraw, x + width, y + height);
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(scale, scale, 1);

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

    public Style getStyleAt(Font font, double mouseX, double mouseY) {
        if (mouseX < viewX || mouseX >= viewX + viewWidth || mouseY < viewY - viewOverdraw || mouseY >= viewY + viewScreenHeight) {
            return null;
        }

        float scale = Theme.getTextScale();
        double textX = (mouseX - viewX) / scale - BOX_PADDING_X;
        double textY = (mouseY - viewY) / scale;
        int blockY = viewHeight - contentHeight + Math.round(animatedScroll);

        for (Block block : blocks) {
            if (textY >= blockY && textY < blockY + block.boxHeight()) {
                if (block.type() != ClientMessageType.AGENT) {
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
        ClientConversation conversation = ClientConversation.getInstance();

        if (conversation.getRevision() == layoutRevision && width == layoutWidth) {
            return;
        }

        int previousHeight = contentHeight;
        int textWidth = (int) (width * MAX_BOX_WIDTH) - BOX_PADDING_X * 2;

        blocks.clear();
        contentHeight = 0;
        latestExchangeHeight = 0;

        for (ClientMessage message : conversation.getMessages()) {
            if (message.getType() != ClientMessageType.STATUS) {
                addBlock(createBlock(font, message, textWidth, conversation));
            }
        }

        if (conversation.isBusy()) {
            addBlock(createBlock(font, new StatusMessage(conversation.getWorkingStatus(), Instant.now()), textWidth, conversation));
        }

        if (scroll > 0 && !anchored && width == layoutWidth) {
            scroll += contentHeight - previousHeight;
            animatedScroll += contentHeight - previousHeight;
        }

        layoutRevision = conversation.getRevision();
        layoutWidth = width;
    }

    private Block createBlock(Font font, ClientMessage message, int textWidth, ClientConversation conversation) {
        boolean voice = message instanceof PlayerMessage playerMessage && playerMessage.isVoice();
        // A capture's bubble before it's transcribed: it shimmers with what voice is doing instead.
        boolean placeholder = voice && message == conversation.getPendingRequest() && message.getText().isEmpty();
        String text = placeholder ? getVoicePlaceholder(conversation) : message.getText();

        return switch (message.getType()) {
            case PLAYER, AGENT, ERROR -> {
                MarkdownLayout.Layout layout = message.getType() == ClientMessageType.AGENT
                        ? MarkdownLayout.layout(font, text, textWidth)
                        : new MarkdownLayout.Layout(getPlainLines(font, text, textWidth), List.of());
                List<TextLine> lines = layout.lines();
                List<MarkdownLayout.Panel> panels = layout.panels();
                int textWidthUsed = Math.max(
                        lines.stream().mapToInt(line -> line.x() + font.width(line.text())).max().orElse(0),
                        panels.stream().mapToInt(panel -> panel.x() + panel.width()).max().orElse(0)
                );
                int textHeight = Math.max(
                        lines.stream().mapToInt(TextLine::y).max().orElse(0) + LINE_HEIGHT,
                        panels.stream().mapToInt(panel -> panel.y() + panel.height() + 1).max().orElse(0)
                );
                int boxWidth = textWidthUsed + BOX_PADDING_X * 2;
                int boxHeight = textHeight - 1 + BOX_PADDING_Y * 2;
                String postfix = getPostfix(message);
                int height = postfix == null ? boxHeight : boxHeight + POSTFIX_GAP + getPostfixHeight();

                yield new Block(message.getType(), lines, panels, boxWidth, boxHeight, postfix, height, voice, placeholder);
            }
            case STATUS -> {
                List<TextLine> lines = getPlainLines(font, message.getText(), (int) (textWidth / getStatusScale()));
                int textLength = lines.stream().mapToInt(line -> font.width(line.text())).max().orElse(0);
                int height = (int) Math.ceil(lines.size() * LINE_HEIGHT * getStatusScale());

                yield new Block(message.getType(), lines, List.of(), textLength, height, null, height, false, false);
            }
        };
    }

    private void addBlock(Block block) {
        contentHeight += (blocks.isEmpty() ? 0 : GAP) + block.height();
        latestExchangeHeight = block.type() == ClientMessageType.PLAYER || blocks.isEmpty()
                ? block.height()
                : latestExchangeHeight + GAP + block.height();
        blocks.add(block);
    }

    private void renderBlock(GuiGraphics graphics, Font font, Block block, int x, int y, int width) {
        switch (block.type()) {
            case PLAYER -> {
                if (block.placeholder()) {
                    renderPlaceholderBox(graphics, font, block, x + width - block.width(), y);
                } else {
                    renderBox(graphics, font, block, x + width - block.width(), y, Theme.PLAYER_BOX, Theme.PLAYER_BOX_BORDER, Theme.PLAYER_TEXT);
                }

                renderPostfix(graphics, font, block, x + width - BOX_PADDING_X, y, true);
            }
            case AGENT -> {
                renderBox(graphics, font, block, x, y, Theme.VIO_BOX, Theme.VIO_BOX_BORDER, Theme.VIO_TEXT);
                renderPostfix(graphics, font, block, x + BOX_PADDING_X, y, false);
            }
            case STATUS -> {
                float statusScale = getStatusScale();

                graphics.pose().pushPose();
                graphics.pose().translate(x + BOX_PADDING_X, y, 0);
                graphics.pose().scale(statusScale, statusScale, 1);

                for (TextLine line : block.lines()) {
                    ShimmerRenderer.render(graphics, font, line.text(), line.y(), block.width());
                }

                graphics.pose().popPose();
            }
            case ERROR -> renderBox(graphics, font, block, x, y, Theme.ERROR_BOX, Theme.ERROR_BOX_BORDER, Theme.ERROR_TEXT);
        }
    }

    private void renderScrollbar(GuiGraphics graphics, int x, int y, int height) {
        int alpha = Math.round(scrollbarOpacity * 255);

        if (alpha <= 0) {
            return;
        }

        int maxScroll = contentHeight - viewHeight;
        int thumbHeight = maxScroll <= 0 ? height : Math.max(8, height * viewHeight / contentHeight);
        float scrolled = Math.max(0, Math.min(animatedScroll, maxScroll));
        int thumbY = maxScroll <= 0 ? y : y + Math.round((height - thumbHeight) * (maxScroll - scrolled) / maxScroll);

        graphics.fill(x, thumbY, x + 1, thumbY + thumbHeight, alpha << 24 | Theme.VIO_TEXT & 0xFFFFFF);
    }

    private void animateScroll() {
        long nanos = Util.getNanos();
        float seconds = Math.min((nanos - lastRenderNanos) / 1.0E9f, MAX_FRAME_SECONDS);
        lastRenderNanos = nanos;

        animatedScroll += (scroll - animatedScroll) * (1 - (float) Math.exp(-SCROLL_EASING * seconds));

        if (Math.abs(scroll - animatedScroll) < 0.5f) {
            animatedScroll = scroll;
        }

        float targetOpacity = isOverflowing() ? 1 : 0;
        scrollbarOpacity += (targetOpacity - scrollbarOpacity) * (1 - (float) Math.exp(-SCROLLBAR_FADE_EASING * seconds));
    }

    private int clampScroll(int value) {
        return Math.max(0, Math.min(value, contentHeight - viewHeight));
    }

    private static void renderBox(GuiGraphics graphics, Font font, Block block, int x, int y, int boxColor, int borderColor, int textColor) {
        BoxRenderer.render(graphics, x, y, block.width(), block.boxHeight(), boxColor, borderColor);

        for (MarkdownLayout.Panel panel : block.panels()) {
            BoxRenderer.render(graphics, x + BOX_PADDING_X + panel.x(), y + BOX_PADDING_Y + panel.y(), panel.width(), panel.height(), Theme.CODE_BACKGROUND, Theme.CODE_BLOCK_BORDER);
        }

        for (TextLine line : block.lines()) {
            int lineX = x + BOX_PADDING_X + line.x();
            int lineY = y + BOX_PADDING_Y + line.y();

            if (block.type() == ClientMessageType.AGENT) {
                renderCodeBackgrounds(graphics, font, line.text(), lineX, lineY);
            }

            graphics.drawString(font, line.text(), lineX, lineY, textColor, true);
        }
    }

    private static void renderPlaceholderBox(GuiGraphics graphics, Font font, Block block, int x, int y) {
        BoxRenderer.render(graphics, x, y, block.width(), block.boxHeight(), Theme.PLAYER_BOX, Theme.PLAYER_BOX_BORDER);

        graphics.pose().pushPose();
        graphics.pose().translate(x + BOX_PADDING_X, y + BOX_PADDING_Y, 0);

        for (TextLine line : block.lines()) {
            ShimmerRenderer.render(graphics, font, line.text(), line.y(), block.width() - BOX_PADDING_X * 2);
        }

        graphics.pose().popPose();
    }

    private static void renderCodeBackgrounds(GuiGraphics graphics, Font font, FormattedCharSequence line, int x, int y) {
        int[] position = {0};
        int[] runStart = {-1};

        line.accept((index, style, codePoint) -> {
            boolean code = InlineMarkdown.isCode(style);

            if (code && runStart[0] < 0) {
                runStart[0] = position[0];
            } else if (!code && runStart[0] >= 0) {
                renderCodeBackground(graphics, x + runStart[0], x + position[0], y);
                runStart[0] = -1;
            }

            position[0] += font.width(FormattedCharSequence.codepoint(codePoint, style));
            return true;
        });

        if (runStart[0] >= 0) {
            renderCodeBackground(graphics, x + runStart[0], x + position[0], y);
        }
    }

    // Boxed like a code block, but around the text where it already is: the border takes the free row above and below
    // the glyphs and the column beside them, so nothing moves.
    private static void renderCodeBackground(GuiGraphics graphics, int left, int right, int y) {
        BoxRenderer.render(graphics, left - 2, y - 1, right - left + 3, LINE_HEIGHT + 1, Theme.CODE_BACKGROUND, Theme.CODE_BLOCK_BORDER);
    }

    private static void renderPostfix(GuiGraphics graphics, Font font, Block block, int x, int y, boolean alignRight) {
        if (block.postfix() == null) {
            return;
        }

        float scale = getStatusScale();
        // Spoken requests are marked with a microphone before the time.
        int glyphWidth = block.voice() ? GlyphRenderer.getWidth(GlyphRenderer.MICROPHONE) + GLYPH_GAP : 0;
        int left = alignRight ? x - (int) Math.ceil((glyphWidth + font.width(block.postfix())) * scale) : x;

        graphics.pose().pushPose();
        graphics.pose().translate(left, y + block.boxHeight() + POSTFIX_GAP, 0);
        graphics.pose().scale(scale, scale, 1);

        if (block.voice()) {
            GlyphRenderer.render(graphics, GlyphRenderer.MICROPHONE, 0, 0, Theme.STATUS_TEXT);
        }

        graphics.drawString(font, block.postfix(), glyphWidth, 0, Theme.STATUS_TEXT, false);
        graphics.pose().popPose();
    }

    private static String getVoicePlaceholder(ClientConversation conversation) {
        String status = conversation.getVoiceStatus();

        return status == null ? I18n.get("vio.voice.listening") : status;
    }

    private static List<TextLine> getPlainLines(Font font, String text, int width) {
        List<TextLine> lines = new ArrayList<>();

        for (FormattedCharSequence line : font.split(FormattedText.of(text), width)) {
            lines.add(new TextLine(line, 0, lines.size() * LINE_HEIGHT));
        }

        return lines;
    }

    private static String getPostfix(ClientMessage message) {
        String time = TIME_FORMAT.format(message.getTime());

        if (message instanceof ThoughtMessage thoughtMessage && thoughtMessage.getThoughtDuration() != null) {
            return time + " | " + getThoughtText(thoughtMessage.getThoughtDuration());
        }

        return switch (message.getType()) {
            case PLAYER, AGENT -> time;
            default -> null;
        };
    }

    private static String getThoughtText(Duration thoughtDuration) {
        long seconds = Math.max(1, (long) Math.ceil(thoughtDuration.toMillis() / 1000.0));

        return seconds == 1 ? I18n.get("vio.chat.thought.one") : I18n.get("vio.chat.thought", seconds);
    }

    private static int getPostfixHeight() {
        return (int) Math.ceil((LINE_HEIGHT - 1) * getStatusScale());
    }

    private static float getStatusScale() {
        return Theme.getStatusTextScale() / Theme.getTextScale();
    }

    private static int getContentWidth(int width) {
        return (int) ((width - SCROLLBAR_GUTTER) / Theme.getTextScale());
    }

    private record Block(ClientMessageType type, List<TextLine> lines, List<MarkdownLayout.Panel> panels, int width, int boxHeight, String postfix, int height, boolean voice, boolean placeholder) {
    }
}
