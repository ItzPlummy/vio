package com.plummy.vio.tool.editing;

import com.plummy.vio.tool.argument.ArgumentDefinition;
import com.plummy.vio.tool.argument.ArgumentType;
import com.plummy.vio.tool.argument.Arguments;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.SequencedMap;

public class FillRegionTool extends FillTool {

    @Override
    public String getName() {
        return "fill_region";
    }

    @Override
    public String getDescription() {
        return "Fill every block between two corners (inclusive), like /fill." + COMMON_DESCRIPTION;
    }

    @Override
    protected void putShapeArguments(SequencedMap<String, ArgumentDefinition> schema) {
        schema.put("from", new ArgumentDefinition(ArgumentType.BLOCK_POSITION, "First corner [x, y, z], inclusive."));
        schema.put("to", new ArgumentDefinition(ArgumentType.BLOCK_POSITION, "Opposite corner [x, y, z], inclusive."));
    }

    @Override
    protected Shape getShape(Arguments arguments) {
        BoundingBox box = BoundingBox.fromCorners(
                arguments.getBlockPosition("from").toBlockPos(),
                arguments.getBlockPosition("to").toBlockPos()
        );

        return new Shape() {
            @Override
            public BoundingBox bounds() {
                return box;
            }

            @Override
            public boolean contains(int x, int y, int z) {
                return box.isInside(x, y, z);
            }
        };
    }
}
