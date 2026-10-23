package com.plummy.vio.tool.editing;

public class FillConeTool extends AxialFillTool {

    @Override
    public String getName() {
        return "fill_cone";
    }

    @Override
    public String getDescription() {
        return "Fill a cone: a round base around center, shrinking linearly to a single-block tip height blocks along the axis (height + 1 layers)." + COMMON_DESCRIPTION;
    }

    @Override
    protected int getLength(int height) {
        return height;
    }

    @Override
    protected boolean contains(int a, int b, int h, int radius, int height) {
        if (h < 0 || h > height) {
            return false;
        }

        double layerRadius = (double) radius * (height - h) / height;

        return a * a + b * b < square(layerRadius + 0.5);
    }
}
