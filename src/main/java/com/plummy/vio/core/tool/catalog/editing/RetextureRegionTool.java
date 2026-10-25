package com.plummy.vio.core.tool.catalog.editing;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.plummy.vio.core.access.AccessLevel;
import com.plummy.vio.core.tool.executor.ExecutionResult;
import com.plummy.vio.core.tool.ServerTool;
import com.plummy.vio.core.tool.value.argument.ArgumentDefinition;
import com.plummy.vio.core.tool.value.argument.ArgumentType;
import com.plummy.vio.core.tool.value.argument.Arguments;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.*;

public class RetextureRegionTool implements ServerTool {

    @Override
    public String getName() {
        return "retexture_region";
    }

    @Override
    public String getDescription() {
        return "Swap one material family for another in a region, keeping every block state (facing, half, shape, waterlogged, ...) and block entity data where both blocks share it, e.g. oak -> spruce turns oak_stairs into spruce_stairs and oak_log into spruce_log. Ids are matched by whole _-separated segments, so oak never matches dark_oak. Only existing target blocks are used; blocks without a counterpart are listed in unmapped and left alone.";
    }

    @Override
    public SequencedMap<String, ArgumentDefinition> getArgumentSchema() {
        SequencedMap<String, ArgumentDefinition> schema = new LinkedHashMap<>();

        schema.put("status", ArgumentDefinition.status());
        schema.put("from", new ArgumentDefinition(ArgumentType.BLOCK_POSITION, "First corner [x, y, z], inclusive."));
        schema.put("to", new ArgumentDefinition(ArgumentType.BLOCK_POSITION, "Opposite corner [x, y, z], inclusive."));
        schema.put("dimension", new ArgumentDefinition(ArgumentType.DIMENSION, "Dimension id."));
        schema.put("from_material", new ArgumentDefinition(ArgumentType.STRING, "Material id segment to replace, e.g. oak, stone_brick, cobblestone."));
        schema.put("to_material", new ArgumentDefinition(ArgumentType.STRING, "Replacement segment, e.g. spruce, deepslate_brick."));

        return schema;
    }

    @Override
    public AccessLevel getAccessLevel() {
        return AccessLevel.FULL;
    }

    @Override
    public ExecutionResult execute(Context context, Arguments arguments) {
        ServerLevel level = context.getLevel(arguments.getDimension("dimension"));
        BoundingBox box = BoundingBox.fromCorners(
                arguments.getBlockPosition("from").toBlockPos(),
                arguments.getBlockPosition("to").toBlockPos()
        );
        List<String> from = segments(arguments.getString("from_material"));
        List<String> to = segments(arguments.getString("to_material"));

        if (from.isEmpty() || to.isEmpty()) {
            return ExecutionResult.invalidArguments("from_material and to_material must not be empty");
        }

        long volume = (long) box.getXSpan() * box.getYSpan() * box.getZSpan();

        if (volume > 262144) {
            return ExecutionResult.invalidArguments("The region is %s blocks (%s×%s×%s), the maximum is 262144".formatted(volume, box.getXSpan(), box.getYSpan(), box.getZSpan()));
        }

        context.requireLoaded(level, box);

        Set<String> paths = new HashSet<>();
        BuiltInRegistries.BLOCK.keySet().forEach(id -> paths.add(id.toString()));

        Map<Block, Optional<Block>> mapping = new LinkedHashMap<>();
        Set<Block> otherBlocks = new HashSet<>();
        LongArrayList positions = new LongArrayList();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (BlockPos cell : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
            Block block = level.getBlockState(cell).getBlock();

            if (otherBlocks.contains(block)) {
                continue;
            }

            Optional<Block> target = mapping.get(block);

            if (target == null) {
                target = map(block, from, to, paths);

                if (target == null) {
                    otherBlocks.add(block);
                    continue;
                }

                mapping.put(block, target);
            }

            if (target.isPresent()) {
                positions.add(cell.asLong());
            }
        }

        if (mapping.isEmpty()) {
            return ExecutionResult.failure("No block in the region has '%s' in its id".formatted(String.join("_", from)));
        }

        for (long cell : positions) {
            BlockState state = level.getBlockState(pos.set(cell));
            BlockState replacement = copyProperties(state, mapping.get(state.getBlock()).orElseThrow().defaultBlockState());
            BlockEntity old = level.getBlockEntity(pos);
            CompoundTag nbt = null;

            if (old != null && isSameBlockEntityType(old, replacement, pos)) {
                nbt = old.saveWithoutMetadata(level.registryAccess());
            }

            // No neighbor updates: sand doesn't fall and water doesn't flow until something else updates them.
            BlockPlacer.place(level, pos, replacement, nbt, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }

        JsonObject mappingJson = new JsonObject();
        List<String> unmapped = new ArrayList<>();

        mapping.forEach((block, target) -> {
            if (target.isPresent()) {
                mappingJson.addProperty(BuiltInRegistries.BLOCK.getKey(block).toString(), BuiltInRegistries.BLOCK.getKey(target.get()).toString());
            } else {
                unmapped.add(BuiltInRegistries.BLOCK.getKey(block).toString());
            }
        });

        JsonObject json = new JsonObject();

        json.addProperty("blocks_placed", positions.size());
        json.add("mapping", mappingJson);
        json.add("unmapped", unmapped.stream().sorted().collect(JsonArray::new, JsonArray::add, JsonArray::addAll));

        return ExecutionResult.success(json);
    }

    // null = not this material at all; empty = this material, but no counterpart exists.
    private static Optional<Block> map(Block block, List<String> from, List<String> to, Set<String> ids) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        List<String> parts = segments(id.getPath());

        for (int i = 0; i + from.size() <= parts.size(); i++) {
            if (!parts.subList(i, i + from.size()).equals(from)) {
                continue;
            }

            List<String> replaced = new ArrayList<>(parts.subList(0, i));
            replaced.addAll(to);
            replaced.addAll(parts.subList(i + from.size(), parts.size()));

            ResourceLocation targetId = id.withPath(String.join("_", replaced));

            if (ids.contains(targetId.toString())) {
                return Optional.of(BuiltInRegistries.BLOCK.get(targetId));
            }

            // With the segment before it, the match names another material (dark_oak for oak), so it isn't this one.
            if (i > 0) {
                String longer = "%s:%s_%s_".formatted(id.getNamespace(), parts.get(i - 1), String.join("_", from));

                if (ids.stream().anyMatch(other -> other.startsWith(longer))) {
                    continue;
                }
            }

            return Optional.empty();
        }

        return null;
    }

    private static List<String> segments(String text) {
        return Arrays.stream(text.strip().toLowerCase(Locale.ROOT).split("_")).filter(part -> !part.isEmpty()).toList();
    }

    // Properties both blocks have, by name and value.
    private static BlockState copyProperties(BlockState from, BlockState to) {
        BlockState result = to;

        for (Property<?> property : from.getProperties()) {
            Property<?> targetProperty = to.getBlock().getStateDefinition().getProperty(property.getName());

            if (targetProperty != null) {
                result = setValue(result, targetProperty, getValueName(from, property));
            }
        }

        return result;
    }

    private static <T extends Comparable<T>> String getValueName(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }

    private static <T extends Comparable<T>> BlockState setValue(BlockState state, Property<T> property, String value) {
        return property.getValue(value).map(parsed -> state.setValue(property, parsed)).orElse(state);
    }

    // Data carries over only between block entities of the same type, e.g. sign to sign.
    private static boolean isSameBlockEntityType(BlockEntity old, BlockState replacement, BlockPos pos) {
        if (!(replacement.getBlock() instanceof EntityBlock entityBlock)) {
            return false;
        }

        BlockEntity created = entityBlock.newBlockEntity(pos, replacement);

        return created != null && created.getType() == old.getType();
    }
}
