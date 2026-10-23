package com.plummy.vio.tool.editing;

import com.plummy.vio.tool.argument.ArgumentDefinition;
import com.plummy.vio.tool.argument.ArgumentType;
import com.plummy.vio.tool.argument.Arguments;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.SequencedMap;

public class FillLineTool extends FillTool {

    @Override
    public String getName() {
        return "fill_line";
    }

    @Override
    public String getDescription() {
        return "Place a one-block-thick straight line from a start to an end position, both included." + COMMON_DESCRIPTION;
    }

    @Override
    protected void putShapeArguments(SequencedMap<String, ArgumentDefinition> schema) {
        schema.put("from", new ArgumentDefinition(ArgumentType.BLOCK_POSITION, "Start [x, y, z]."));
        schema.put("to", new ArgumentDefinition(ArgumentType.BLOCK_POSITION, "End [x, y, z]."));
    }

    @Override
    protected Shape getShape(Arguments arguments) {
        BlockPos start = arguments.getBlockPosition("from").toBlockPos();
        BlockPos end = arguments.getBlockPosition("to").toBlockPos();
        LongArrayList cells = step(start, end);

        // Stepped directly instead of scanning its box, which can be huge for a long diagonal line.
        return new Shape() {
            @Override
            public BoundingBox bounds() {
                return BoundingBox.fromCorners(start, end);
            }

            @Override
            public boolean contains(int x, int y, int z) {
                return cells.contains(BlockPos.asLong(x, y, z));
            }

            @Override
            public LongArrayList getPositions(long maxVolume) {
                return cells;
            }

            @Override
            public void requireLoaded(Context context, ServerLevel level) {
                for (long cell : cells) {
                    context.requireLoaded(level, BlockPos.of(cell));
                }
            }
        };
    }

    // The cells a stepped line visits, one per step along its longest axis.
    private static LongArrayList step(BlockPos start, BlockPos end) {
        int dx = end.getX() - start.getX();
        int dy = end.getY() - start.getY();
        int dz = end.getZ() - start.getZ();
        int steps = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
        LongArrayList positions = new LongArrayList();

        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0.0 : (double) i / steps;
            positions.add(BlockPos.asLong((int) Math.round(start.getX() + dx * t), (int) Math.round(start.getY() + dy * t), (int) Math.round(start.getZ() + dz * t)));
        }

        return positions;
    }
}
