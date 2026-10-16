package com.plummy.vio.client.hud;

import com.plummy.vio.client.FrameRenderer;
import com.plummy.vio.client.cube.CubeController;
import com.plummy.vio.client.cube.CubeRenderer;
import net.minecraft.client.gui.GuiGraphics;

public final class IconRenderer {

    private static final int UNIT = 15;

    public static final int CLOSED_SIZE = UNIT * 2;
    public static final int OPEN_SIZE = UNIT * 3;

    private static final int PADDING = 2;
    private static final float Z_OFFSET = 50;

    private IconRenderer() {
    }

    public static int getX(int screenWidth, int size) {
        return (screenWidth - size) / 2;
    }

    public static int getY(int size) {
        return -size / 2;
    }

    public static int getBottom(int size) {
        return getY(size) + size;
    }

    public static boolean contains(int screenWidth, int size, double mouseX, double mouseY) {
        int x = getX(screenWidth, size);
        int y = getY(size);

        return mouseX >= x && mouseX < x + size && mouseY >= y && mouseY < y + size;
    }

    public static void render(GuiGraphics graphics, int size) {
        int x = getX(graphics.guiWidth(), size);
        int y = getY(size);

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, Z_OFFSET);

        FrameRenderer.render(graphics, x, y, size, size, size / 2);

        CubeController cube = CubeController.getInstance();
        cube.update();
        CubeRenderer.render(graphics, x + PADDING, y + PADDING, size - PADDING * 2, cube);

        graphics.pose().popPose();
    }
}
