package com.plummy.vio.tool.catalog.world;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.plummy.vio.access.AccessLevel;
import com.plummy.vio.tool.ServerTool;
import com.plummy.vio.tool.executor.ExecutionResult;
import com.plummy.vio.tool.value.argument.ArgumentDefinition;
import com.plummy.vio.tool.value.argument.ArgumentType;
import com.plummy.vio.tool.value.argument.Arguments;
import com.plummy.vio.tool.value.catalog.ItemList;
import com.plummy.vio.tool.value.catalog.ItemSummary;
import com.plummy.vio.tool.value.result.ResultValueJsonSerializer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

import java.util.*;

public class GetContainerContentsTool implements ServerTool {

    @Override
    public String getName() {
        return "get_container_contents";
    }

    @Override
    public String getDescription() {
        return "Contents of a container block (chest, barrel, shulker box, hopper, dispenser, furnace, brewing stand, ...) with a per-item summary. Double chests are merged. Furnaces show input, fuel, output and cook progress; brewing stands their bottles, ingredient, fuel and brew time. An unopened loot chest shows only loot_table_pending, since reading it would generate the loot. For container entities (chest minecarts, donkeys) use get_entity_info.";
    }

    @Override
    public SequencedMap<String, ArgumentDefinition> getArgumentSchema() {
        SequencedMap<String, ArgumentDefinition> schema = new LinkedHashMap<>();

        schema.put("status", ArgumentDefinition.status());
        schema.put("position", new ArgumentDefinition(ArgumentType.BLOCK_POSITION, "Container position [x, y, z]."));
        schema.put("dimension", new ArgumentDefinition(ArgumentType.DIMENSION, "Dimension id."));

        return schema;
    }

    @Override
    public AccessLevel getAccessLevel() {
        return AccessLevel.BASIC;
    }

    @Override
    public ExecutionResult execute(Context context, Arguments arguments) {
        ServerLevel level = context.getLevel(arguments.getDimension("dimension"));

        BlockPos pos = arguments.getBlockPosition("position").toBlockPos();
        context.requireLoaded(level, pos);

        BlockState state = level.getBlockState(pos);
        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (!(blockEntity instanceof Container single)) {
            return ExecutionResult.invalidArguments("%s at %s %s %s (%s) isn't a container".formatted(BuiltInRegistries.BLOCK.getKey(state.getBlock()), pos.getX(), pos.getY(), pos.getZ(), level.dimension().location()));
        }

        Container container = single;
        boolean isDouble = false;
        List<BlockEntity> parts = new ArrayList<>(List.of(blockEntity));

        if (state.getBlock() instanceof ChestBlock chest && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            BlockPos otherPos = pos.relative(ChestBlock.getConnectedDirection(state));

            if (level.isLoaded(otherPos)) {
                Container combined = ChestBlock.getContainer(chest, state, level, pos, true);

                if (combined != null && combined != single) {
                    container = combined;
                    isDouble = true;
                    parts.add(level.getBlockEntity(otherPos));
                }
            }
        }

        JsonObject json = new JsonObject();

        json.addProperty("container", BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
        json.addProperty("double", isDouble);
        json.addProperty("slots_total", container.getContainerSize());

        for (BlockEntity part : parts) {
            if (part instanceof RandomizableContainer randomizable && randomizable.getLootTable() != null) {
                json.add("items", new JsonArray());
                json.add("summary", new JsonObject());
                json.addProperty("loot_table_pending", randomizable.getLootTable().location().toString());
                return ExecutionResult.success(json);
            }
        }

        Map<Integer, ItemStack> stacks = new HashMap<>();

        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            stacks.put(slot, container.getItem(slot));
        }

        if (blockEntity instanceof AbstractFurnaceBlockEntity) {
            CompoundTag tag = blockEntity.saveWithoutMetadata(level.registryAccess());
            JsonObject progress = new JsonObject();

            progress.addProperty("cook_time_ticks", tag.getInt("CookTime"));
            progress.addProperty("cook_time_total_ticks", tag.getInt("CookTimeTotal"));
            progress.addProperty("burn_time_left_ticks", tag.getInt("BurnTime"));

            json.add("input", new ItemSummary(stacks.get(0)).accept(ResultValueJsonSerializer.getInstance()));
            json.add("fuel", new ItemSummary(stacks.get(1)).accept(ResultValueJsonSerializer.getInstance()));
            json.add("output", new ItemSummary(stacks.get(2)).accept(ResultValueJsonSerializer.getInstance()));
            json.add("cook_progress", progress);
        } else if (blockEntity instanceof BrewingStandBlockEntity) {
            CompoundTag tag = blockEntity.saveWithoutMetadata(level.registryAccess());

            json.add("bottles", new ItemList(Map.of(0, stacks.get(0), 1, stacks.get(1), 2, stacks.get(2))).accept(ResultValueJsonSerializer.getInstance()));
            json.add("ingredient", new ItemSummary(stacks.get(3)).accept(ResultValueJsonSerializer.getInstance()));
            json.add("fuel", new ItemSummary(stacks.get(4)).accept(ResultValueJsonSerializer.getInstance()));
            json.addProperty("brew_time_left_ticks", tag.getShort("BrewTime"));
            json.addProperty("fuel_charges", tag.getByte("Fuel"));
        } else {
            json.add("items", new ItemList(stacks).accept(ResultValueJsonSerializer.getInstance()));
        }

        Map<String, Integer> totals = new TreeMap<>();

        for (ItemStack stack : stacks.values()) {
            if (!stack.isEmpty()) {
                totals.merge(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), stack.getCount(), Integer::sum);
            }
        }

        JsonObject summary = new JsonObject();
        totals.forEach(summary::addProperty);

        json.add("summary", summary);
        json.add("loot_table_pending", null);

        return ExecutionResult.success(json);
    }
}
