package com.plummy.vio.client.screen;

import com.mojang.blaze3d.platform.Window;
import com.plummy.vio.client.BoxRenderer;
import com.plummy.vio.client.FrameRenderer;
import com.plummy.vio.client.Theme;
import com.plummy.vio.client.conversation.ClientConversation;
import com.plummy.vio.client.hud.IconRenderer;
import com.plummy.vio.client.message.ClientMessage;
import com.plummy.vio.client.message.ClientMessageType;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Whence;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

public class VioScreen extends Screen {

    private static final float WIDTH = 0.4f;
    private static final float MIN_HEIGHT = 0.24f;
    private static final float MAX_HEIGHT = 0.8f;
    private static final int MIN_WIDTH = 200;
    private static final int SCREEN_MARGIN = 4;
    private static final int WINDOW_TOP = 3;
    private static final int ICON_GAP = 3;
    private static final int PADDING = 5;
    private static final int CORNER_RADIUS = 3;
    private static final int INPUT_GAP = 4;
    private static final int INPUT_PADDING_X = 3;
    private static final int INPUT_PADDING_Y = 2;
    private static final int MIN_MESSAGES_HEIGHT = 18;
    private static final int SCROLL_LINES = 3;
    private static final long ANIMATION_MILLIS = 600;
    private static final float HEIGHT_EASING = 8f;
    private static final float MAX_FRAME_SECONDS = 0.1f;
    private static final float FADE_Z = 10;

    private static final long HOLD_BASE_MILLIS = 1000;
    private static final long HOLD_MILLIS_PER_CHARACTER = 40;

    private static VioScreen detached;

    private final ClientConversation conversation = ClientConversation.getInstance();
    private final MessageList messages = new MessageList();

    private InputField input;

    private final long openedMillis = Util.getMillis();
    private final float openedFrom;

    private boolean openingLink;

    private boolean pinned;
    private boolean answering;
    private boolean sent;
    private long closedMillis = -1;
    private float closedFrom;

    private int scrollGrowth;

    private float animatedHeight = -1;
    private long lastLayoutNanos;
    private int targetHeight;
    private int maxHeight;
    private int targetMessagesHeight;

    private int referenceWidth;
    private int referenceHeight;

    private int windowX;
    private int windowY;
    private int windowWidth;
    private int windowHeight;
    private int messagesY;
    private int messagesHeight;
    private int inputY;
    private int visibleInputLines;

    public VioScreen() {
        super(Component.translatable("vio.screen.title"));

        openedFrom = detached == null ? 0 : detached.getProgress();

        if (detached != null) {
            animatedHeight = detached.animatedHeight;
            messages.continueFrom(detached.messages);
            answering = detached.pinned;
        } else if (conversation.isBusy()) {
            answering = true;
            messages.anchorToLatestExchange();
        }

        detached = null;
    }

    public static boolean renderDetached(GuiGraphics graphics) {
        if (detached == null) {
            return false;
        }

        if (detached.pinned && !detached.conversation.isBusy() && Util.getMillis() >= detached.getHoldUntilMillis()) {
            detached.startClosing();
        }

        if (detached.closedMillis >= 0 && detached.getProgress() <= 0) {
            detached = null;
            return false;
        }

        detached.renderWindow(graphics);
        return true;
    }

    public static void clearDetached() {
        detached = null;
    }

    public static Style getDetachedStyleAt(double mouseX, double mouseY) {
        return detached == null ? null : detached.messages.getStyleAt(detached.font, mouseX, mouseY);
    }

    @Override
    protected void init() {
        String value = input == null ? conversation.getDraft() : input.value();
        int cursor = input == null ? value.length() : input.cursor();

        Window window = Minecraft.getInstance().getWindow();
        double autoScale = window.calculateScale(0, Minecraft.getInstance().isEnforceUnicode());
        referenceWidth = (int) (window.getWidth() / autoScale);
        referenceHeight = (int) (window.getHeight() / autoScale);

        windowWidth = Math.min(Math.max(Math.round(referenceWidth * WIDTH), MIN_WIDTH), width - SCREEN_MARGIN * 2);
        input = new InputField(font, windowWidth - PADDING * 2 - INPUT_PADDING_X * 2);
        input.setValue(value);
        input.seekCursor(Whence.ABSOLUTE, cursor);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderWindow(graphics);
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        switch (keyCode) {
            case GLFW.GLFW_KEY_ESCAPE -> {
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                if (hasShiftDown()) {
                    input.insertText("\n");
                    input.onEdit();
                } else {
                    send();
                }

                return true;
            }
            case GLFW.GLFW_KEY_PAGE_UP -> {
                scroll(messagesHeight);
                return true;
            }
            case GLFW.GLFW_KEY_PAGE_DOWN -> {
                scroll(-messagesHeight);
                return true;
            }
        }

        if (input.keyPressed(keyCode)) {
            input.onEdit();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (!StringUtil.isAllowedChatCharacter(codePoint)) {
            return false;
        }

        input.insertText(Character.toString(codePoint));
        input.onEdit();
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll((int) Math.round(scrollY * input.getScreenLineHeight() * SCROLL_LINES));
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int boxX = windowX + PADDING;
        int textHeight = visibleInputLines * input.getScreenLineHeight();

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            Style style = messages.getStyleAt(font, mouseX, mouseY);

            if (style != null && style.getClickEvent() != null) {
                openingLink = true;

                try {
                    return handleComponentClicked(style);
                } finally {
                    openingLink = false;
                }
            }
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT
                && mouseX >= boxX && mouseX < boxX + windowWidth - PADDING * 2
                && mouseY >= inputY - INPUT_PADDING_Y && mouseY < inputY + textHeight + INPUT_PADDING_Y) {
            input.setSelecting(hasShiftDown());
            input.clickAt(mouseX - boxX - INPUT_PADDING_X, Math.clamp(mouseY - inputY, 0, textHeight - 1));
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void removed() {
        conversation.setDraft(input.value());

        if (openingLink) {
            return;
        }

        detached = this;
        pinned = sent && (conversation.isBusy() || Util.getMillis() < getHoldUntilMillis());

        if (!pinned) {
            startClosing();
        }
    }

    private void renderWindow(GuiGraphics graphics) {
        if (closedMillis < 0) {
            updateLayout();
        }

        float progress = getProgress();
        int slide = Math.round((1 - progress) * (windowY + windowHeight));

        FrameRenderer.render(graphics, windowX, windowY - slide, windowWidth, windowHeight, CORNER_RADIUS);

        int contentX = windowX + PADDING;
        int contentWidth = windowWidth - PADDING * 2;
        int inputBoxHeight = visibleInputLines * input.getScreenLineHeight() + INPUT_PADDING_Y * 2;

        int fadeTop = windowY + FrameRenderer.BORDER - slide;
        int fadeHeight = messagesY - FrameRenderer.BORDER - windowY;

        messages.render(graphics, font, contentX, messagesY - slide, contentWidth, messagesHeight, fadeHeight);
        renderTopFade(graphics, contentX, fadeTop, contentWidth, fadeHeight);
        renderInputBox(graphics, contentX, inputY - INPUT_PADDING_Y - slide, contentWidth, inputBoxHeight);
        input.render(graphics, contentX + INPUT_PADDING_X, inputY - slide, visibleInputLines, getPlaceholder());

        int iconSize = Math.round(IconRenderer.CLOSED_SIZE + (IconRenderer.OPEN_SIZE - IconRenderer.CLOSED_SIZE) * progress);
        IconRenderer.render(graphics, iconSize);
    }

    private void renderTopFade(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, FADE_Z);
        graphics.fillGradient(x, y, x + width, y + height, Theme.BACKGROUND, Theme.BACKGROUND & 0xFFFFFF);
        graphics.pose().popPose();
    }

    private void renderInputBox(GuiGraphics graphics, int x, int y, int width, int height) {
        float scale = input.getScale();
        int border = minecraft != null && minecraft.screen == this ? Theme.INPUT_BORDER_FOCUSED : Theme.INPUT_BORDER;

        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(scale, scale, 1);
        BoxRenderer.render(graphics, 0, 0, (int) (width / scale + 0.001f), (int) (height / scale + 0.001f), Theme.INPUT_BOX, border);
        graphics.pose().popPose();
    }

    private void startClosing() {
        closedFrom = getProgress();
        closedMillis = Util.getMillis();
        pinned = false;
    }

    private void send() {
        if (conversation.send(input.value())) {
            input.setValue("");
            scrollGrowth = 0;
            answering = true;
            sent = true;
            messages.anchorToLatestExchange();

            onClose();
        }
    }

    private void scroll(int amount) {
        if (amount > 0) {
            int room = maxHeight - targetHeight;
            int hidden = messages.getHiddenAbove(targetMessagesHeight);
            int grow = Math.max(0, Math.min(amount, Math.min(room, hidden)));

            scrollGrowth += grow;
            messages.scrollBy(amount - grow);
        } else {
            int unscroll = Math.min(-amount, messages.getScreenScroll());

            messages.scrollBy(-unscroll);
            scrollGrowth = Math.max(0, scrollGrowth - (-amount - unscroll));
        }
    }

    private void updateLayout() {
        windowX = (width - windowWidth) / 2;
        windowY = WINDOW_TOP;

        messagesY = Math.max(windowY + PADDING, IconRenderer.getBottom(IconRenderer.OPEN_SIZE) + ICON_GAP);

        int availableHeight = height - windowY - SCREEN_MARGIN;
        int chrome = messagesY - windowY + PADDING + INPUT_GAP + INPUT_PADDING_Y * 2;
        int lineHeight = input.getScreenLineHeight();
        int inputLines = Math.max(1, input.getLineCount());
        int extraInputHeight = (inputLines - 1) * lineHeight;
        int exchangeHeight = chrome + lineHeight + messages.getLatestExchangeHeight(font, windowWidth - PADDING * 2);
        int minHeight;
        int wantedHeight;

        maxHeight = Math.min(Math.round(referenceHeight * MAX_HEIGHT), availableHeight);

        if (minecraft != null && minecraft.screen == this) {
            minHeight = Math.min(Math.round(referenceHeight * MIN_HEIGHT), maxHeight);
            wantedHeight = (answering ? exchangeHeight : minHeight) + extraInputHeight + scrollGrowth;
        } else {
            minHeight = Math.min(chrome + lineHeight, maxHeight);
            wantedHeight = exchangeHeight + extraInputHeight;
        }

        targetHeight = Math.max(minHeight, Math.min(wantedHeight, maxHeight));
        windowHeight = animateHeight(targetHeight);

        int reservedMessagesHeight = Math.max(0, Math.min(MIN_MESSAGES_HEIGHT, minHeight - chrome - lineHeight));
        int maxInputLines = Math.max(1, (targetHeight - chrome - reservedMessagesHeight) / lineHeight);
        visibleInputLines = Math.min(inputLines, maxInputLines);
        targetMessagesHeight = targetHeight - chrome - visibleInputLines * lineHeight;

        inputY = windowY + windowHeight - PADDING - INPUT_PADDING_Y - visibleInputLines * lineHeight;
        messagesHeight = inputY - INPUT_PADDING_Y - INPUT_GAP - messagesY;
    }

    private int animateHeight(int target) {
        long nanos = Util.getNanos();
        float seconds = Math.min((nanos - lastLayoutNanos) / 1.0E9f, MAX_FRAME_SECONDS);
        lastLayoutNanos = nanos;

        if (animatedHeight < 0) {
            animatedHeight = target;
        } else {
            animatedHeight += (target - animatedHeight) * (1 - (float) Math.exp(-HEIGHT_EASING * seconds));
        }

        return Math.round(animatedHeight);
    }

    private float getProgress() {
        if (closedMillis >= 0) {
            return closedFrom * (1 - ease(closedMillis));
        }

        return openedFrom + (1 - openedFrom) * ease(openedMillis);
    }

    private long getHoldUntilMillis() {
        ClientMessage last = conversation.getLastMessage();

        if (last == null || (last.getType() != ClientMessageType.AGENT && last.getType() != ClientMessageType.ERROR)) {
            return 0;
        }

        return conversation.getLastChangeMillis() + HOLD_BASE_MILLIS + last.getText().length() * HOLD_MILLIS_PER_CHARACTER;
    }

    private String getPlaceholder() {
        if (!conversation.isAvailable()) {
            return I18n.get("vio.chat.error.not_installed");
        }

        return I18n.get(conversation.isBusy() ? "vio.chat.placeholder.busy" : "vio.chat.placeholder");
    }

    private static float ease(long startMillis) {
        float t = Math.min(1, (Util.getMillis() - startMillis) / (float) ANIMATION_MILLIS);

        return 1 - (1 - t) * (1 - t) * (1 - t);
    }
}
