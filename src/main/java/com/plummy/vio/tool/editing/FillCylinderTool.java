package com.plummy.vio.tool.editing;

public class FillCylinderTool extends AxialFillTool {

    @Override
    public String getName() {
        return "fill_cylinder";
    }

    @Override
    public String getDescription() {
        return "Fill a cylinder: a round base around center, height layers long along the axis." + COMMON_DESCRIPTION;
    }

    @Override
    protected int getLength(int height) {
        return height - 1;
    }

    @Override
    protected boolean contains(int a, int b, int h, int radius, int height) {
        return h >= 0 && h < height && a * a + b * b < square(radius + 0.5);
    }
}
