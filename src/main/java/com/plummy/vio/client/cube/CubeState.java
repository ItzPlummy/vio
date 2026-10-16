package com.plummy.vio.client.cube;

import com.plummy.vio.client.conversation.ClientConversation;
import com.plummy.vio.client.message.ClientMessage;
import com.plummy.vio.client.message.ClientMessageType;
import net.minecraft.Util;

public enum CubeState {

    IDLE(0.6f, 21, 31, 1f, false, 0f),
    THINKING(2.5f, 19, 29, 0.8f, true, 0f),
    WORKING(4.0f, 19, 29, 0.8f, true, 0f),
    ERROR(0.6f, 21, 31, 1f, false, 1f);

    private static final long FLASH_MILLIS = 1000;

    private final float spinSpeed;
    private final int minGrid;
    private final int maxGrid;
    private final float size;
    private final boolean axisLocked;
    private final float alert;

    CubeState(float spinSpeed, int minGrid, int maxGrid, float size, boolean axisLocked, float alert) {
        this.spinSpeed = spinSpeed;
        this.minGrid = minGrid;
        this.maxGrid = maxGrid;
        this.size = size;
        this.axisLocked = axisLocked;
        this.alert = alert;
    }

    public static CubeState fromConversation(ClientConversation conversation) {
        ClientMessage last = conversation.getLastMessage();
        ClientMessageType type = last == null ? null : last.getType();

        if (conversation.isBusy()) {
            return type == ClientMessageType.STATUS ? WORKING : THINKING;
        }

        if (type == ClientMessageType.ERROR && Util.getMillis() - conversation.getLastChangeMillis() < FLASH_MILLIS) {
            return ERROR;
        }

        return IDLE;
    }

    public float getSpinSpeed() {
        return spinSpeed;
    }

    public int getMinGrid() {
        return minGrid;
    }

    public int getMaxGrid() {
        return maxGrid;
    }

    public float getSize() {
        return size;
    }

    public boolean isAxisLocked() {
        return axisLocked;
    }

    public float getAlert() {
        return alert;
    }
}
