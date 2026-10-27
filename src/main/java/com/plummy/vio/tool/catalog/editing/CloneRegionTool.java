package com.plummy.vio.tool.catalog.editing;

import com.google.gson.JsonObject;
import com.plummy.vio.access.AccessLevel;
import com.plummy.vio.tool.ServerTool;
import com.plummy.vio.tool.executor.ExecutionResult;
import com.plummy.vio.tool.value.argument.ArgumentDefinition;
import com.plummy.vio.tool.value.argument.ArgumentType;
import com.plummy.vio.tool.value.argument.Arguments;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;

public class CloneRegionTool implements ServerTool {

    @Override
    public String getName() {
        return "clone_region";
    }

    @Override
    public String getDescription() {
        return "/clone without its size cap, copying across dimensions and optionally entities. The copy keeps its orientation, with its minimum corner at destination. move clears the source after copying (no item drops).";
    }

    @Override
    public SequencedMap<String, ArgumentDefinition> getArgumentSchema() {
        SequencedMap<String, ArgumentDefinition> schema = new LinkedHashMap<>();

        schema.put("status", ArgumentDefinition.status());
        schema.put("from", new ArgumentDefinition(ArgumentType.BLOCK_POSITION, "Source corner [x, y, z], inclusive."));
        schema.put("to", new ArgumentDefinition(ArgumentType.BLOCK_POSITION, "Opposite source corner [x, y, z], inclusive."));
        schema.put("dimension", new ArgumentDefinition(ArgumentType.DIMENSION, "Source dimension id."));
        schema.put("destination", new ArgumentDefinition(ArgumentType.BLOCK_POSITION, "Destination minimum corner [x, y, z]."));
        schema.put("dest_dimension", new ArgumentDefinition(ArgumentType.DIMENSION, "Destination dimension id."));
        schema.put("mode", ArgumentDefinition.enumeration(Mode.class, "copy, or move (clears the source afterwards)."));
        schema.put("include_entities", new ArgumentDefinition(ArgumentType.BOOLEAN, "Whether non-player entities in the source are copied (moved with move)."));
        schema.put("skip_air", new ArgumentDefinition(ArgumentType.BOOLEAN, "Whether air in the source leaves destination blocks alone."));

        return schema;
    }

    @Override
    public AccessLevel getAccessLevel() {
        return AccessLevel.FULL;
    }

    @Override
    public ExecutionResult execute(Context context, Arguments arguments) {
        ServerLevel source = context.getLevel(arguments.getDimension("dimension"));
        ServerLevel destination = context.getLevel(arguments.getDimension("dest_dimension"));

        BoundingBox box = BoundingBox.fromCorners(
                arguments.getBlockPosition("from").toBlockPos(),
                arguments.getBlockPosition("to").toBlockPos()
        );
        BlockPos destOrigin = arguments.getBlockPosition("destination").toBlockPos();
        boolean move = arguments.getEnum("mode", Mode.class) == Mode.MOVE;
        boolean includeEntities = arguments.getBoolean("include_entities");
        boolean skipAir = arguments.getBoolean("skip_air");

        long volume = (long) box.getXSpan() * box.getYSpan() * box.getZSpan();

        if (volume > 262144) {
            return ExecutionResult.invalidArguments("The region is %s blocks (%s×%s×%s), the maximum is 262144".formatted(volume, box.getXSpan(), box.getYSpan(), box.getZSpan()));
        }

        context.requireLoaded(source, box);

        BoundingBox destBox = box.moved(destOrigin.getX() - box.minX(), destOrigin.getY() - box.minY(), destOrigin.getZ() - box.minZ());

        context.requireLoaded(destination, destBox);

        if (move && source == destination && box.intersects(destBox)) {
            return ExecutionResult.invalidArguments("With mode move, the source and destination must not overlap in the same dimension");
        }

        List<Long> destPositions = new ArrayList<>();
        List<BlockState> states = new ArrayList<>();
        List<CompoundTag> blockData = new ArrayList<>();
        List<Long> sourcePositions = new ArrayList<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int y = box.minY(); y <= box.maxY(); y++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                for (int x = box.minX(); x <= box.maxX(); x++) {
                    BlockState state = source.getBlockState(pos.set(x, y, z));
                    sourcePositions.add(pos.asLong());

                    if (skipAir && state.isAir()) {
                        continue;
                    }

                    BlockEntity blockEntity = source.getBlockEntity(pos);
                    destPositions.add(destOrigin.offset(x - box.minX(), y - box.minY(), z - box.minZ()).asLong());
                    states.add(state);
                    blockData.add(blockEntity == null ? null : blockEntity.saveWithoutMetadata(source.registryAccess()));
                }
            }
        }

        List<Entity> sourceEntities = includeEntities
                ? source.getEntities((Entity) null, new AABB(box.minX(), box.minY(), box.minZ(), box.maxX() + 1, box.maxY() + 1, box.maxZ() + 1), entity -> !(entity instanceof Player) && !entity.isPassenger())
                : List.of();
        List<CompoundTag> entityData = new ArrayList<>();
        Vec3 sourceMin = new Vec3(box.minX(), box.minY(), box.minZ());

        for (Entity entity : sourceEntities) {
            CompoundTag tag = new CompoundTag();

            if (entity.save(tag)) {
                tag.put("Pos", vec(Vec3.atLowerCornerOf(destOrigin).add(entity.position().subtract(sourceMin))));
                removeUuids(tag);
                entityData.add(tag);
            }
        }

        for (int i = 0; i < destPositions.size(); i++) {
            BlockPlacer.place(destination, pos.set(destPositions.get(i)), states.get(i), blockData.get(i), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }

        for (CompoundTag tag : entityData) {
            Entity entity = EntityType.loadEntityRecursive(tag, destination, loaded -> loaded);

            if (entity != null) {
                destination.addFreshEntityWithPassengers(entity);
            }
        }

        if (move) {
            for (Entity entity : sourceEntities) {
                entity.getPassengersAndSelf().forEach(Entity::discard);
            }

            for (long sourcePos : sourcePositions) {
                BlockPlacer.place(source, pos.set(sourcePos), Blocks.AIR.defaultBlockState(), null, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
        }

        JsonObject json = new JsonObject();

        json.addProperty("blocks_placed", destPositions.size());
        json.addProperty("entities_placed", entityData.size());

        return ExecutionResult.success(json);
    }

    private static ListTag vec(Vec3 pos) {
        ListTag list = new ListTag();

        list.add(DoubleTag.valueOf(pos.x));
        list.add(DoubleTag.valueOf(pos.y));
        list.add(DoubleTag.valueOf(pos.z));

        return list;
    }

    private static void removeUuids(CompoundTag tag) {
        tag.remove("UUID");

        for (Tag passenger : tag.getList("Passengers", Tag.TAG_COMPOUND)) {
            removeUuids((CompoundTag) passenger);
        }
    }

    private enum Mode {
        COPY,
        MOVE
    }
}
