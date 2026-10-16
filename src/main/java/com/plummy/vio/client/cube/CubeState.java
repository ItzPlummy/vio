package com.plummy.vio.client.cube;

import com.plummy.vio.client.ChatEntry;
import com.plummy.vio.client.ClientChatHistory;
import net.minecraft.Util;

// Visual targets per agent state. CubeController eases toward them, so switching states never jumps.
public enum CubeState {

    //         spin   grid     size   axis locked  alert
    IDLE(      0.6f,  21, 31,  1f,    false,       0f),
    THINKING(  2.5f,  19, 29,  0.8f,  true,        0f),
    WORKING(   4.0f,  19, 29,  0.8f,  true,        0f),
    ERROR(     0.6f,  21, 31,  1f,    false,       1f);

    // How long ERROR lasts after the error arrives.
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

    public static CubeState of(ClientChatHistory history) {
        ChatEntry last = history.getLastEntry();
        ChatEntry.Kind kind = last == null ? null : last.getKind();

        if (history.isBusy()) {
            return kind == ChatEntry.Kind.STATUS ? WORKING : THINKING;
        }

        if (kind == ChatEntry.Kind.ERROR && Util.getMillis() - history.getLastChangeMillis() < FLASH_MILLIS) {
            return ERROR;
        }

        return IDLE;
    }

    // Radians per second.
    public float getSpinSpeed() {
        return spinSpeed;
    }

    // Cells across the icon.
    public int getMinGrid() {
        return minGrid;
    }

    public int getMaxGrid() {
        return maxGrid;
    }

    // Fraction of the full cube size. Only the cube shrinks: the cells stay the same, so the pixelation does not change.
    public float getSize() {
        return size;
    }

    // Whether the cube keeps spinning around the axis it had when the state began, instead of drifting.
    public boolean isAxisLocked() {
        return axisLocked;
    }

    // 0-1, how much the edges turn to the accent color.
    public float getAlert() {
        return alert;
    }
}
