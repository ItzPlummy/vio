package com.plummy.vio.client.cube;

import com.plummy.vio.client.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Vector3f;

import java.util.Arrays;

public final class CubeRenderer {

    private static final float[][] VERTICES = {
            {-1, -1, -1}, {1, -1, -1}, {1, 1, -1}, {-1, 1, -1},
            {-1, -1, 1}, {1, -1, 1}, {1, 1, 1}, {-1, 1, 1}
    };

    private static final int[][] EDGES = {
            {0, 1}, {1, 2}, {2, 3}, {3, 0},
            {4, 5}, {5, 6}, {6, 7}, {7, 4},
            {0, 4}, {1, 5}, {2, 6}, {3, 7}
    };

    public static final int EDGE_COUNT = EDGES.length;

    private static final float CAMERA_DISTANCE = 5;
    private static final float CORNER_DISTANCE = (float) Math.sqrt(3);
    private static final float FIT_EXTENT = 2.2f;

    private CubeRenderer() {
    }

    public static void render(GuiGraphics graphics, int x, int y, int size, CubeController cube) {
        double guiScale = Minecraft.getInstance().getWindow().getGuiScale();

        int realSize = (int) Math.floor(size * guiScale);
        int cellSize = Math.max(1, realSize / cube.getGrid());
        int cells = realSize / cellSize;

        if (cells < 2) {
            return;
        }

        float[] depth = new float[cells * cells];
        int[] edges = new int[cells * cells];
        rasterize(cube, cells, depth, edges);

        int originX = (int) Math.round(x * guiScale) + (realSize - cells * cellSize) / 2;
        int originY = (int) Math.round(y * guiScale) + (realSize - cells * cellSize) / 2;

        graphics.pose().pushPose();
        graphics.pose().scale((float) (1 / guiScale), (float) (1 / guiScale), 1);

        for (int row = 0; row < cells; row++) {
            for (int column = 0; column < cells; column++) {
                float value = depth[row * cells + column];

                if (value < 0) {
                    continue;
                }

                int left = originX + column * cellSize;
                int top = originY + row * cellSize;

                graphics.fill(left, top, left + cellSize, top + cellSize, getColor(value, cube.getEdgeAccent(edges[row * cells + column]), cube));
            }
        }

        graphics.pose().popPose();
    }

    private static void rasterize(CubeController cube, int cells, float[] depth, int[] edges) {
        Arrays.fill(depth, -1);

        float center = (cells - 1) / 2f;
        float scale = center / FIT_EXTENT * cube.getSize();

        float[] screenX = new float[VERTICES.length];
        float[] screenY = new float[VERTICES.length];
        float[] nearness = new float[VERTICES.length];

        for (int i = 0; i < VERTICES.length; i++) {
            Vector3f vertex = cube.getOrientation().transform(new Vector3f(VERTICES[i][0], VERTICES[i][1], VERTICES[i][2]));
            float perspective = CAMERA_DISTANCE / (CAMERA_DISTANCE - vertex.z);

            screenX[i] = center + vertex.x * perspective * scale;
            screenY[i] = center - vertex.y * perspective * scale;
            nearness[i] = (vertex.z + CORNER_DISTANCE) / (2 * CORNER_DISTANCE);
        }

        for (int edge = 0; edge < EDGES.length; edge++) {
            int from = EDGES[edge][0];
            int to = EDGES[edge][1];
            int steps = Math.max(1, (int) Math.ceil(Math.max(Math.abs(screenX[to] - screenX[from]), Math.abs(screenY[to] - screenY[from]))));

            for (int step = 0; step <= steps; step++) {
                float t = (float) step / steps;
                int column = Math.round(screenX[from] + (screenX[to] - screenX[from]) * t);
                int row = Math.round(screenY[from] + (screenY[to] - screenY[from]) * t);

                if (column < 0 || row < 0 || column >= cells || row >= cells) {
                    continue;
                }

                int index = row * cells + column;
                float near = nearness[from] + (nearness[to] - nearness[from]) * t;

                if (near > depth[index]) {
                    depth[index] = near;
                    edges[index] = edge;
                }
            }
        }
    }

    private static int getColor(float nearness, float edgeAccent, CubeController cube) {
        int color = Theme.lerp(Theme.CUBE_BACK, Theme.CUBE_FRONT, nearness);
        color = Theme.lerp(color, Theme.CUBE_ACCENT, edgeAccent);

        return Theme.lerp(color, Theme.CUBE_ACCENT, cube.getAlert() * 0.8f);
    }
}
