package com.plummy.vio.client.hud;

import com.plummy.vio.client.VioFrame;
import com.plummy.vio.client.cube.CubeController;
import com.plummy.vio.client.cube.CubeRenderer;
import net.minecraft.client.gui.GuiGraphics;

// The cube in its rounded frame, at the top center of the screen.
public class VioIcon {

    // Size unit: the closed icon is 2 units, the open one 3.
    private static final int UNIT = 15;

    public static final int CLOSED_SIZE = UNIT * 2;
    public static final int OPEN_SIZE = UNIT * 3;

    private static final int PADDING = 2;
    private static final float Z_OFFSET = 50;

    private VioIcon() {
    }

    public static int getX(int screenWidth, int size) {
        return (screenWidth - size) / 2;
    }

    // The center sits on the top edge of the screen, so only the lower half is visible, open or closed.
    public static int getY(int size) {
        return -size / 2;
    }

    // Row just below the icon, for laying out what goes below it.
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

        // Raised above the window: shadowed text is drawn slightly in front of z = 0, so flat fills at z = 0
        // would lose the depth test to any text under the icon, even though the icon is drawn later.
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, Z_OFFSET);

        // A corner radius of half the side makes it a circle.
        VioFrame.render(graphics, x, y, size, size, size / 2);

        CubeController cube = CubeController.getInstance();
        cube.update();
        CubeRenderer.render(graphics, x + PADDING, y + PADDING, size - PADDING * 2, cube);

        graphics.pose().popPose();
    }
}
