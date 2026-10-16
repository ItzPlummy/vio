package com.plummy.vio.client.screen;

import com.mojang.blaze3d.platform.Window;
import com.plummy.vio.client.ChatEntry;
import com.plummy.vio.client.ClientChatHistory;
import com.plummy.vio.client.VioBox;
import com.plummy.vio.client.VioFrame;
import com.plummy.vio.client.VioTheme;
import com.plummy.vio.client.hud.VioIcon;
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

    // Window size as fractions of the screen at the automatic GUI scale, where it was tuned. Measured there, the window
    // has a fixed size in GUI pixels, so it grows and shrinks with the GUI scale like the rest of the GUI.
    private static final float WIDTH = 0.4f;
    // Focused, the window is at least this tall; reading older messages or a long input grows it up to the maximum.
    private static final float MIN_HEIGHT = 0.24f;
    private static final float MAX_HEIGHT = 0.8f;
    private static final int MIN_WIDTH = 200;
    private static final int SCREEN_MARGIN = 4;
    // The window starts this close to the top edge; the icon overlaps it.
    private static final int WINDOW_TOP = 3;
    private static final int ICON_GAP = 3;
    private static final int PADDING = 5;
    private static final int CORNER_RADIUS = 3;
    // Space between the messages and the input box.
    private static final int INPUT_GAP = 4;
    // Space between the input box's edge and its text.
    private static final int INPUT_PADDING_X = 3;
    private static final int INPUT_PADDING_Y = 2;
    private static final int MIN_MESSAGES_HEIGHT = 18;
    // Lines of input-sized text per wheel step.
    private static final int SCROLL_LINES = 3;
    private static final long ANIMATION_MILLIS = 600;
    private static final float HEIGHT_EASING = 8f;
    private static final float MAX_FRAME_SECONDS = 0.1f;
    // Lifts the top fade over the messages: shadowed text is drawn slightly in front of z = 0, so a fill at z = 0
    // would lose the depth test to it. Still below the icon.
    private static final float FADE_Z = 10;

    // How long the window stays up after a reply: 1 s plus 0.04 s per character.
    private static final long HOLD_BASE_MILLIS = 1000;
    private static final long HOLD_MILLIS_PER_CHARACTER = 40;

    // A closed screen the HUD layer still draws: pinned while Vio works and the reply is read, then closing.
    private static VioScreen detached;

    private final ClientChatHistory history = ClientChatHistory.getInstance();
    private final MessageList messages = new MessageList();

    private InputField input;

    // Set once per opening, so a resize does not replay the animation.
    private final long openedMillis = Util.getMillis();
    // Above 0 when reopened mid-close, so the animation turns around instead of restarting.
    private final float openedFrom;

    // Set while a link is being opened: vanilla may show its "open this link?" screen, which comes back to this one,
    // so this window should not detach and close meanwhile.
    private boolean openingLink;

    private boolean pinned;
    // Set while this window follows an exchange: from sending a message until its reply has been read.
    private boolean answering;
    // Set when the window closes because a message was sent; only then does it stay up while Vio answers.
    private boolean sent;
    private long closedMillis = -1;
    private float closedFrom;

    // How much scrolling up has grown the focused window, in screen pixels. Older messages first come into view
    // by the window growing, up to its maximum, and only then by scrolling.
    private int scrollGrowth;

    // The height eases toward the layout's target every frame.
    private float animatedHeight = -1;
    private long lastLayoutNanos;
    private int targetHeight;
    private int maxHeight;
    private int targetMessagesHeight;

    // The screen size in GUI pixels at the automatic GUI scale; the window size fractions apply to it.
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

        // Reopening the window the HUD still shows continues from its current height, scroll and exchange.
        if (detached != null) {
            animatedHeight = detached.animatedHeight;
            messages.continueFrom(detached.messages);
            answering = detached.pinned;
        } else if (history.isBusy()) {
            answering = true;
            messages.anchorToLatestExchange();
        }

        detached = null;
    }

    // Called by the HUD layer. Returns false once the detached window is fully gone.
    public static boolean renderDetached(GuiGraphics graphics) {
        if (detached == null) {
            return false;
        }

        if (detached.pinned && !detached.history.isBusy() && Util.getMillis() >= detached.getHoldUntilMillis()) {
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

    @Override
    protected void init() {
        // Also called on resize, so the text and cursor carry over to the field with the new width.
        String value = input == null ? history.getDraft() : input.value();
        int cursor = input == null ? value.length() : input.cursor();

        // Also called when the GUI scale changes, so the reference size follows it.
        Window window = Minecraft.getInstance().getWindow();
        double autoScale = window.calculateScale(0, Minecraft.getInstance().isEnforceUnicode());
        referenceWidth = (int) (window.getWidth() / autoScale);
        referenceHeight = (int) (window.getHeight() / autoScale);

        // Never wider than the screen, even when that is narrower than the minimum.
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

    private void renderWindow(GuiGraphics graphics) {
        // While closing, the window keeps its last size instead of snapping to another one.
        if (closedMillis < 0) {
            updateLayout();
        }

        float progress = getProgress();
        // The window slides down from behind the top edge; layout and input use the final position.
        int slide = Math.round((1 - progress) * (windowY + windowHeight));

        VioFrame.render(graphics, windowX, windowY - slide, windowWidth, windowHeight, CORNER_RADIUS);

        int contentX = windowX + PADDING;
        int contentWidth = windowWidth - PADDING * 2;
        int inputBoxHeight = visibleInputLines * input.getScreenLineHeight() + INPUT_PADDING_Y * 2;

        // Messages scroll on under the icon, up to the top edge of the window, fading out above the clear area.
        int fadeTop = windowY + VioFrame.BORDER - slide;
        int fadeHeight = messagesY - VioFrame.BORDER - windowY;

        messages.render(graphics, font, contentX, messagesY - slide, contentWidth, messagesHeight, fadeHeight);
        renderTopFade(graphics, contentX, fadeTop, contentWidth, fadeHeight);
        renderInputBox(graphics, contentX, inputY - INPUT_PADDING_Y - slide, contentWidth, inputBoxHeight);
        input.render(graphics, contentX + INPUT_PADDING_X, inputY - slide, visibleInputLines, getPlaceholder());

        // Drawn last, so it overlaps the window's top edge. It grows from the HUD size to the open size and back.
        int iconSize = Math.round(VioIcon.CLOSED_SIZE + (VioIcon.OPEN_SIZE - VioIcon.CLOSED_SIZE) * progress);
        VioIcon.render(graphics, iconSize);
    }

    // The window background, from fully transparent at the bottom of the area to opaque at its top, over the messages.
    private void renderTopFade(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, FADE_Z);
        graphics.fillGradient(x, y, x + width, y + height, VioTheme.BACKGROUND, VioTheme.BACKGROUND & 0xFFFFFF);
        graphics.pose().popPose();
    }

    // Drawn in text space, so its border matches the message boxes. Position and size in screen pixels.
    private void renderInputBox(GuiGraphics graphics, int x, int y, int width, int height) {
        float scale = input.getScale();
        // Violet only while the window has focus; the HUD-drawn window takes no typing.
        int border = minecraft != null && minecraft.screen == this ? VioTheme.INPUT_BORDER_FOCUSED : VioTheme.INPUT_BORDER;

        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(scale, scale, 1);
        // The small margin keeps float error (e.g. 200 / (2/3) = 299.99997) from losing a pixel.
        VioBox.render(graphics, 0, 0, (int) (width / scale + 0.001f), (int) (height / scale + 0.001f), VioTheme.INPUT_BOX, border);
        graphics.pose().popPose();
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // No blur or dimming: the game stays visible around the window.
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
                // Vanilla checks the chat link settings and the scheme, and asks for confirmation if set to.
                openingLink = true;

                try {
                    return handleComponentClicked(style);
                } finally {
                    openingLink = false;
                }
            }
        }

        // The whole box is clickable; a click in its padding goes to the nearest visible line.
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
        history.setDraft(input.value());

        if (openingLink) {
            return;
        }

        // The game gets control back right away and the HUD layer keeps drawing the window.
        // After sending, it stays up while Vio works and the reply is read, so the player can move meanwhile.
        // Closing it any other way (Esc, even mid-answer) hides it, so a long reply can be put away.
        detached = this;
        pinned = sent && (history.isBusy() || Util.getMillis() < getHoldUntilMillis());

        if (!pinned) {
            startClosing();
        }
    }

    private void startClosing() {
        closedFrom = getProgress();
        closedMillis = Util.getMillis();
        pinned = false;
    }

    // Keeps the window up long enough to read the reply (or the error) that ended the request.
    private long getHoldUntilMillis() {
        ChatEntry last = history.getLastEntry();

        if (last == null || (last.getKind() != ChatEntry.Kind.VIO && last.getKind() != ChatEntry.Kind.ERROR)) {
            return 0;
        }

        return history.getLastChangeMillis() + HOLD_BASE_MILLIS + last.getText().length() * HOLD_MILLIS_PER_CHARACTER;
    }

    private void send() {
        if (history.send(input.value())) {
            input.setValue("");
            scrollGrowth = 0;
            answering = true;
            sent = true;
            messages.anchorToLatestExchange();

            // Hands control back to the game; the window stays up until the reply has been read.
            onClose();
        }
    }

    // Positive scrolls up into older messages: the window grows first (only as far as there is more to show),
    // then the messages scroll. Scrolling down undoes it in reverse: the messages first, then the window shrinks.
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

    // 0 when hidden, 1 when fully open.
    private float getProgress() {
        if (closedMillis >= 0) {
            return closedFrom * (1 - ease(closedMillis));
        }

        return openedFrom + (1 - openedFrom) * ease(openedMillis);
    }

    // Eased out (cubic), so the motion settles softly.
    private static float ease(long startMillis) {
        float t = Math.min(1, (Util.getMillis() - startMillis) / (float) ANIMATION_MILLIS);

        return 1 - (1 - t) * (1 - t) * (1 - t);
    }

    private void updateLayout() {
        windowX = (width - windowWidth) / 2;
        windowY = WINDOW_TOP;

        // The clear message area starts below the icon. Older messages scroll on above it, fading out under the icon.
        messagesY = Math.max(windowY + PADDING, VioIcon.getBottom(VioIcon.OPEN_SIZE) + ICON_GAP);

        int availableHeight = height - windowY - SCREEN_MARGIN;
        int chrome = messagesY - windowY + PADDING + INPUT_GAP + INPUT_PADDING_Y * 2;
        int lineHeight = input.getScreenLineHeight();
        int inputLines = Math.max(1, input.getLineCount());
        int extraInputHeight = (inputLines - 1) * lineHeight;
        // Just enough for the latest exchange: the player's message, then statuses and the reply as they arrive.
        int exchangeHeight = chrome + lineHeight + messages.getLatestExchangeHeight(font, windowWidth - PADDING * 2);
        int minHeight;
        int wantedHeight;

        maxHeight = Math.min(Math.round(referenceHeight * MAX_HEIGHT), availableHeight);

        if (minecraft != null && minecraft.screen == this) {
            // Focused: between the minimum and maximum height. Opened mid-exchange, it starts from the exchange's size;
            // a long input or reading older messages grows it.
            minHeight = Math.min(Math.round(referenceHeight * MIN_HEIGHT), maxHeight);
            wantedHeight = (answering ? exchangeHeight : minHeight) + extraInputHeight + scrollGrowth;
        } else {
            // Answering (shown by the HUD while Vio works and the reply is read): only as tall as the exchange,
            // up to the maximum. A longer reply is cut at the bottom, not scrolled, until the player scrolls down.
            minHeight = Math.min(chrome + lineHeight, maxHeight);
            wantedHeight = exchangeHeight + extraInputHeight;
        }

        targetHeight = Math.max(minHeight, Math.min(wantedHeight, maxHeight));
        windowHeight = animateHeight(targetHeight);

        // Input lines follow the target, so a new line shows up at once and the window catches up around it.
        // The messages keep at most what they had with a one-line input, so every extra input line
        // gets the height the window grew by, until the window hits its maximum.
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

    private String getPlaceholder() {
        if (!history.isAvailable()) {
            return I18n.get("vio.chat.error.not_installed");
        }

        return I18n.get(history.isBusy() ? "vio.chat.placeholder.busy" : "vio.chat.placeholder");
    }
}
