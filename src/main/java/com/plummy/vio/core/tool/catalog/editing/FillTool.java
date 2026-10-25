package com.plummy.vio.core.tool.catalog.editing;

import com.google.gson.JsonObject;
import com.plummy.vio.core.access.AccessLevel;
import com.plummy.vio.core.tool.executor.ExecutionResult;
import com.plummy.vio.core.tool.executor.ExecutionResultException;
import com.plummy.vio.core.tool.ServerTool;
import com.plummy.vio.core.tool.value.argument.*;
import com.plummy.vio.core.tool.value.catalog.BlockPredicate;
import com.plummy.vio.core.tool.value.catalog.BlockSpec;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.LinkedHashMap;
import java.util.SequencedMap;

public abstract class FillTool implements ServerTool {

    private static final long MAX_BOUNDS_FACTOR = 8;

    protected static final String COMMON_DESCRIPTION = " No vanilla /fill size cap. Block NBT is copied into every placed block entity, so filling chests with Items duplicates those items.";

    protected abstract void putShapeArguments(SequencedMap<String, ArgumentDefinition> schema);

    protected abstract Shape getShape(Arguments arguments);

    @Override
    public SequencedMap<String, ArgumentDefinition> getArgumentSchema() {
        SequencedMap<String, ArgumentDefinition> schema = new LinkedHashMap<>();

        schema.put("status", ArgumentDefinition.status());
        putShapeArguments(schema);
        schema.put("block", new ArgumentDefinition(ArgumentType.BLOCK_SPEC, "Block like /fill: id with optional [state] and {nbt}, e.g. minecraft:oak_log[axis=y]."));
        schema.put("replace_filter", new ArgumentDefinition(ArgumentType.BLOCK_PREDICATE, "Block predicate (id, #tag, partial state); only matching blocks are replaced; empty = replace everything."));
        schema.put("dimension", new ArgumentDefinition(ArgumentType.DIMENSION, "Dimension id."));

        return schema;
    }

    @Override
    public AccessLevel getAccessLevel() {
        return AccessLevel.FULL;
    }

    @Override
    public ExecutionResult execute(Context context, Arguments arguments) {
        ServerLevel level = context.getLevel(arguments.getDimension("dimension"));
        Shape shape = getShape(arguments);
        BlockSpec block = arguments.getBlockSpec("block");
        BlockState state = block.getState(level);
        CompoundTag nbt = block.getNbt(level);
        BlockPredicate filter = arguments.isEmpty("replace_filter") ? null : arguments.getBlockPredicate("replace_filter");
        long maxVolume = 262144;

        LongArrayList positions = shape.getPositions(maxVolume);

        if (positions.size() > maxVolume) {
            return ExecutionResult.invalidArguments("The shape is %s blocks, over the %s-block edit limit".formatted(positions.size(), maxVolume));
        }

        shape.requireLoaded(context, level);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int placed = 0;

        for (long cell : positions) {
            pos.set(cell);

            if (filter != null && !filter.matches(level, pos, level.getBlockState(pos))) {
                continue;
            }

            BlockPlacer.place(level, pos, state, nbt, Block.UPDATE_ALL);
            placed++;
        }

        JsonObject json = new JsonObject();

        json.addProperty("blocks_placed", placed);

        return ExecutionResult.success(json);
    }

    protected interface Shape {

        BoundingBox bounds();

        boolean contains(int x, int y, int z);

        default LongArrayList getPositions(long maxVolume) {
            BoundingBox bounds = bounds();
            long volume = (long) bounds.getXSpan() * bounds.getYSpan() * bounds.getZSpan();

            if (volume > maxVolume * MAX_BOUNDS_FACTOR) {
                throw ExecutionResultException.invalidArguments("The shape's bounding box is %s blocks, far over the %s-block edit limit".formatted(volume, maxVolume));
            }

            LongArrayList positions = new LongArrayList();

            for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
                for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                    for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
                        if (contains(x, y, z)) {
                            positions.add(BlockPos.asLong(x, y, z));
                        }
                    }
                }
            }

            return positions;
        }

        default void requireLoaded(Context context, ServerLevel level) {
            context.requireLoaded(level, bounds());
        }
    }
}
