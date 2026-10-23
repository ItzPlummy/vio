package com.plummy.vio.tool.editing;

import com.plummy.vio.tool.argument.ArgumentDefinition;
import com.plummy.vio.tool.argument.ArgumentType;
import com.plummy.vio.tool.argument.Arguments;
import com.plummy.vio.tool.argument.BlockPosition;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.SequencedMap;

public class FillEllipsoidTool extends FillTool {

    @Override
    public String getName() {
        return "fill_ellipsoid";
    }

    @Override
    public String getDescription() {
        return "Fill an ellipsoid around a center, with a radius along each axis; equal radii make a sphere. A cell is inside when it lies within the radii (radius 0 = one block thick along that axis)." + COMMON_DESCRIPTION;
    }

    @Override
    protected void putShapeArguments(SequencedMap<String, ArgumentDefinition> schema) {
        schema.put("center", new ArgumentDefinition(ArgumentType.BLOCK_POSITION, "Center [x, y, z]."));
        schema.put("radius_x", new ArgumentDefinition(ArgumentType.INTEGER, "Radius along X."));
        schema.put("radius_y", new ArgumentDefinition(ArgumentType.INTEGER, "Radius along Y."));
        schema.put("radius_z", new ArgumentDefinition(ArgumentType.INTEGER, "Radius along Z."));
    }

    @Override
    protected Shape getShape(Arguments arguments) {
        BlockPosition center = arguments.getBlockPosition("center");
        int cx = center.getX();
        int cy = center.getY();
        int cz = center.getZ();
        int rx = arguments.getInt("radius_x", 0, Integer.MAX_VALUE);
        int ry = arguments.getInt("radius_y", 0, Integer.MAX_VALUE);
        int rz = arguments.getInt("radius_z", 0, Integer.MAX_VALUE);

        return new Shape() {
            @Override
            public BoundingBox bounds() {
                return new BoundingBox(cx - rx, cy - ry, cz - rz, cx + rx, cy + ry, cz + rz);
            }

            @Override
            public boolean contains(int x, int y, int z) {
                return square((x - cx) / (rx + 0.5)) + square((y - cy) / (ry + 0.5)) + square((z - cz) / (rz + 0.5)) < 1.0;
            }
        };
    }

    private static double square(double value) {
        return value * value;
    }
}
