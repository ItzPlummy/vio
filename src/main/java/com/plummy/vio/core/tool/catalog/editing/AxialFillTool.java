package com.plummy.vio.core.tool.catalog.editing;

import com.plummy.vio.core.tool.value.argument.ArgumentDefinition;
import com.plummy.vio.core.tool.value.argument.ArgumentType;
import com.plummy.vio.core.tool.value.argument.Arguments;
import com.plummy.vio.core.tool.value.catalog.BlockPosition;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.SequencedMap;

// Cylinders, cones and pyramids: a base centered on (x,y,z) and a height running along an axis.
public abstract class AxialFillTool extends FillTool {

    // Layers along the axis after the base layer.
    protected abstract int getLength(int height);

    // Whether a cell is inside, from its offsets: a and b across the axis, h along it from the base.
    protected abstract boolean contains(int a, int b, int h, int radius, int height);

    @Override
    protected void putShapeArguments(SequencedMap<String, ArgumentDefinition> schema) {
        schema.put("center", new ArgumentDefinition(ArgumentType.BLOCK_POSITION, "Base center [x, y, z]."));
        schema.put("radius", new ArgumentDefinition(ArgumentType.INTEGER, "Base radius (0 = one block)."));
        schema.put("height", new ArgumentDefinition(ArgumentType.INTEGER, "Height, at least 1."));
        schema.put("axis", ArgumentDefinition.enumeration(Axis.class, "Direction the shape grows from its base: y is up, x is east, z is south."));
    }

    @Override
    protected Shape getShape(Arguments arguments) {
        BlockPosition center = arguments.getBlockPosition("center");
        int cx = center.getX();
        int cy = center.getY();
        int cz = center.getZ();
        int radius = arguments.getInt("radius", 0, Integer.MAX_VALUE);
        int height = arguments.getInt("height", 1, Integer.MAX_VALUE);
        Axis axis = arguments.getEnum("axis", Axis.class);
        int length = getLength(height);

        return new Shape() {
            @Override
            public BoundingBox bounds() {
                return switch (axis) {
                    case Y -> new BoundingBox(cx - radius, cy, cz - radius, cx + radius, cy + length, cz + radius);
                    case X -> new BoundingBox(cx, cy - radius, cz - radius, cx + length, cy + radius, cz + radius);
                    case Z -> new BoundingBox(cx - radius, cy - radius, cz, cx + radius, cy + radius, cz + length);
                };
            }

            @Override
            public boolean contains(int x, int y, int z) {
                int dx = x - cx;
                int dy = y - cy;
                int dz = z - cz;
                int h = switch (axis) {
                    case Y -> dy;
                    case X -> dx;
                    case Z -> dz;
                };

                return AxialFillTool.this.contains(axis == Axis.X ? dy : dx, axis == Axis.Z ? dy : dz, h, radius, height);
            }
        };
    }

    protected static double square(double value) {
        return value * value;
    }

    private enum Axis {
        X,
        Y,
        Z
    }
}
