package com.plummy.vio.tool.catalog.editing;

public class FillPyramidTool extends AxialFillTool {

    @Override
    public String getName() {
        return "fill_pyramid";
    }

    @Override
    public String getDescription() {
        return "Fill a pyramid: a square base around center with half-width radius, shrinking linearly to a single-block apex height blocks along the axis (height + 1 layers).%s".formatted(COMMON_DESCRIPTION);
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

        double halfWidth = Math.floor((double) radius * (height - h) / height);

        return Math.abs(a) <= halfWidth && Math.abs(b) <= halfWidth;
    }
}
