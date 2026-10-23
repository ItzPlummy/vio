package com.plummy.vio.client.hud;

import com.plummy.vio.client.FrameRenderer;
import com.plummy.vio.client.ShimmerRenderer;
import com.plummy.vio.client.Theme;
import com.plummy.vio.client.conversation.ClientConversation;
import com.plummy.vio.client.message.ClientMessage;
import com.plummy.vio.client.message.ClientMessageType;
import com.plummy.vio.client.screen.VioScreen;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

// One line under the icon saying what Vio is doing while no window shows it, sliding in from the top like the window.
public class StatusWindow {

    private static final StatusWindow INSTANCE = new StatusWindow();

    private static final int MARGIN = 3;
    private static final int PADDING_X = 5;
    private static final int PADDING_Y = 4;
    private static final int CORNER_RADIUS = 3;
    private static final float WIDTH_EASING = 8f;
    private static final float MAX_FRAME_SECONDS = 0.1f;

    // Kept while sliding away, so the text doesn't vanish before the window does.
    private Status shown;
    private boolean open;
    private long changedMillis;
    private float changedFrom;

    private float animatedWidth = -1;
    private long lastNanos = Util.getNanos();

    // Where it was last drawn to, sliding included; 0 while hidden.
    private int bottom;

    public static StatusWindow getInstance() {
        return INSTANCE;
    }

    private StatusWindow() {
    }

    // Drawn after the icon but below it, so it slides out from behind it.
    public void render(GuiGraphics graphics, int iconSize) {
        Status status = VioScreen.isShown() ? null : getStatus(ClientConversation.getInstance());

        if (status != null) {
            if (getProgress() <= 0) {
                animatedWidth = -1;
            }

            shown = status;
        }

        setOpen(status != null);

        float progress = getProgress();
        bottom = 0;

        if (progress <= 0 || shown == null) {
            return;
        }

        Font font = Minecraft.getInstance().font;
        float scale = Theme.getTextScale();
        int textLength = font.width(shown.text());
        int textWidth = (int) Math.ceil(textLength * scale);

        int width = animateWidth(textWidth + PADDING_X * 2);
        int height = (int) Math.ceil((font.lineHeight - 1) * scale) + PADDING_Y * 2;
        int x = (graphics.guiWidth() - width) / 2;
        int top = IconRenderer.getBottom(iconSize) + MARGIN;
        int y = top - Math.round((1 - progress) * (top + height));
        bottom = y + height;

        FrameRenderer.render(graphics, x, y, width, height, CORNER_RADIUS);

        // While the width eases, the text is cut to the inside instead of spilling over the frame.
        graphics.enableScissor(x + FrameRenderer.BORDER, y, x + width - FrameRenderer.BORDER, y + height);
        graphics.pose().pushPose();
        graphics.pose().translate((graphics.guiWidth() - textWidth) / 2f, y + PADDING_Y, 0);
        graphics.pose().scale(scale, scale, 1);

        if (shown.error()) {
            graphics.drawString(font, shown.text(), 0, 0, Theme.ERROR_TEXT, true);
        } else {
            ShimmerRenderer.render(graphics, font, FormattedCharSequence.forward(shown.text(), Style.EMPTY), 0, textLength);
        }

        graphics.pose().popPose();
        graphics.disableScissor();
    }

    public int getBottom() {
        return bottom;
    }

    private void setOpen(boolean open) {
        if (this.open != open) {
            changedFrom = getProgress();
            changedMillis = Util.getMillis();
            this.open = open;
        }
    }

    private float getProgress() {
        return open
                ? changedFrom + (1 - changedFrom) * VioScreen.ease(changedMillis)
                : changedFrom * (1 - VioScreen.ease(changedMillis));
    }

    private int animateWidth(int target) {
        long nanos = Util.getNanos();
        float seconds = Math.min((nanos - lastNanos) / 1.0E9f, MAX_FRAME_SECONDS);
        lastNanos = nanos;

        if (animatedWidth < 0) {
            animatedWidth = target;
        } else {
            animatedWidth += (target - animatedWidth) * (1 - (float) Math.exp(-WIDTH_EASING * seconds));
        }

        return Math.round(animatedWidth);
    }

    // A fresh error first, then what voice is doing, then what the agent is working on.
    private static Status getStatus(ClientConversation conversation) {
        ClientMessage last = conversation.getLastMessage();

        if (last != null && last.getType() == ClientMessageType.ERROR
                && Util.getMillis() < conversation.getLastChangeMillis() + VioScreen.getReadingMillis(last.getText())) {
            return new Status(last.getText(), true);
        }

        String voiceStatus = conversation.getVoiceStatus();

        if (voiceStatus != null) {
            return new Status(voiceStatus, false);
        }

        String workingStatus = conversation.getWorkingStatus();

        return workingStatus == null ? null : new Status(workingStatus, false);
    }

    private record Status(String text, boolean error) {
    }
}
