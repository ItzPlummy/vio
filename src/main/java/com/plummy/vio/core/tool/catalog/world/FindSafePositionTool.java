package com.plummy.vio.core.tool.catalog.world;

import com.google.gson.JsonObject;
import com.plummy.vio.core.access.AccessLevel;
import com.plummy.vio.core.tool.executor.ExecutionResult;
import com.plummy.vio.core.tool.ServerTool;
import com.plummy.vio.core.tool.value.argument.ArgumentDefinition;
import com.plummy.vio.core.tool.value.argument.ArgumentType;
import com.plummy.vio.core.tool.value.argument.Arguments;
import com.plummy.vio.core.tool.value.catalog.BlockPosition;
import com.plummy.vio.core.tool.value.result.ResultValueJsonSerializer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class FindSafePositionTool implements ServerTool {

    private static final int MAX_RADIUS = 64;

    private static final Set<Block> HAZARDS = Set.of(
            Blocks.LAVA, Blocks.FIRE, Blocks.SOUL_FIRE, Blocks.MAGMA_BLOCK, Blocks.CAMPFIRE, Blocks.SOUL_CAMPFIRE,
            Blocks.CACTUS, Blocks.SWEET_BERRY_BUSH, Blocks.POWDER_SNOW, Blocks.WITHER_ROSE
    );

    @Override
    public String getName() {
        return "find_safe_position";
    }

    @Override
    public String getDescription() {
        return "Nearest spot to a desired position where a player can safely stand: a solid, non-hazardous floor with two free, harmless blocks above (no lava, fire, magma, campfires, cactus, berry bushes, powder snow or wither roses), inside the world border, in loaded chunks. Use it before teleports. The returned pos is where the player's feet go.";
    }

    @Override
    public SequencedMap<String, ArgumentDefinition> getArgumentSchema() {
        SequencedMap<String, ArgumentDefinition> schema = new LinkedHashMap<>();

        schema.put("status", ArgumentDefinition.status());
        schema.put("position", new ArgumentDefinition(ArgumentType.BLOCK_POSITION, "Desired position [x, y, z], y at the feet."));
        schema.put("dimension", new ArgumentDefinition(ArgumentType.DIMENSION, "Dimension id."));
        schema.put("search_radius_blocks", new ArgumentDefinition(ArgumentType.INTEGER, "How far to search, 0 to 64; 16 is a good default."));

        return schema;
    }

    @Override
    public AccessLevel getAccessLevel() {
        return AccessLevel.BASIC;
    }

    @Override
    public ExecutionResult execute(Context context, Arguments arguments) {
        ServerLevel level = context.getLevel(arguments.getDimension("dimension"));
        BlockPos target = arguments.getBlockPosition("position").toBlockPos();
        int radius = arguments.getInt("search_radius_blocks", 0, MAX_RADIUS);

        context.requireLoaded(level, target);

        // Columns nearest first; once a column is farther away horizontally than the best spot, nothing after it can win.
        List<int[]> columns = new ArrayList<>();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz <= radius * radius) {
                    columns.add(new int[]{dx, dz});
                }
            }
        }

        columns.sort(Comparator.<int[]>comparingInt(column -> column[0] * column[0] + column[1] * column[1]).thenComparingInt(column -> column[0]).thenComparingInt(column -> column[1]));

        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        int minY = Math.max(level.getMinBuildHeight() + 1, target.getY() - radius);
        int maxY = Math.min(level.getMaxBuildHeight() - 2, target.getY() + radius);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int[] column : columns) {
            double horizontal = Math.sqrt(column[0] * column[0] + column[1] * column[1]);

            if (horizontal > bestDistance) {
                break;
            }

            int x = target.getX() + column[0];
            int z = target.getZ() + column[1];

            if (!level.getChunkSource().hasChunk(x >> 4, z >> 4)) {
                continue;
            }

            for (int y = minY; y <= maxY; y++) {
                pos.set(x, y, z);

                if (!isSafe(level, pos)) {
                    continue;
                }

                double distance = Math.sqrt(pos.distSqr(target));

                if (distance < bestDistance) {
                    best = pos.immutable();
                    bestDistance = distance;
                }
            }
        }

        if (best == null) {
            return ExecutionResult.failure("No safe spot within %s blocks of %s %s %s (%s); try a larger radius or a different spot".formatted(radius, target.getX(), target.getY(), target.getZ(), level.dimension().location()));
        }

        JsonObject json = new JsonObject();

        json.add("pos", BlockPosition.of(best).accept(ResultValueJsonSerializer.getInstance()));
        json.addProperty("standing_on", BuiltInRegistries.BLOCK.getKey(level.getBlockState(best.below()).getBlock()).toString());
        json.addProperty("offset_blocks", Math.round(bestDistance * 10) / 10.0);

        return ExecutionResult.success(json);
    }

    private static boolean isSafe(ServerLevel level, BlockPos feet) {
        if (!level.getWorldBorder().isWithinBounds(feet)) {
            return false;
        }

        BlockPos floorPos = feet.below();
        BlockState floor = level.getBlockState(floorPos);

        if (HAZARDS.contains(floor.getBlock()) || !floor.isFaceSturdy(level, floorPos, Direction.UP)) {
            return false;
        }

        return isFree(level, feet, false) && isFree(level, feet.above(), true);
    }

    // Passable and harmless; the head can't be underwater either.
    private static boolean isFree(ServerLevel level, BlockPos pos, boolean head) {
        BlockState state = level.getBlockState(pos);

        if (HAZARDS.contains(state.getBlock()) || state.is(BlockTags.FIRE) || !state.getCollisionShape(level, pos).isEmpty()) {
            return false;
        }

        if (state.getFluidState().is(FluidTags.LAVA)) {
            return false;
        }

        return !head || state.getFluidState().isEmpty();
    }
}
