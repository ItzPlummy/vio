package com.plummy.vio.core.tool.knowledge;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.plummy.vio.core.access.AccessLevel;
import com.plummy.vio.core.tool.ExecutionResult;
import com.plummy.vio.core.tool.ServerTool;
import com.plummy.vio.core.tool.argument.ArgumentDefinition;
import com.plummy.vio.core.tool.argument.ArgumentType;
import com.plummy.vio.core.tool.argument.Arguments;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;
import java.util.stream.Stream;

public class GetBlockTypeInfoTool implements ServerTool {

    @Override
    public String getName() {
        return "get_block_type_info";
    }

    @Override
    public String getDescription() {
        return "A block type (not a placed block): every state property with its valid values, default state, hardness, blast resistance, light emission, the tools that mine it, the lowest tool tier that drops it, its loot table and its item. Use it before placing blocks with specific states.";
    }

    @Override
    public SequencedMap<String, ArgumentDefinition> getArgumentSchema() {
        SequencedMap<String, ArgumentDefinition> schema = new LinkedHashMap<>();

        schema.put("block", new ArgumentDefinition(ArgumentType.IDENTIFIER, "Block id."));

        return schema;
    }

    @Override
    public AccessLevel getAccessLevel() {
        return AccessLevel.BASIC;
    }

    @Override
    public ExecutionResult execute(Context context, Arguments arguments) {
        Holder.Reference<Block> holder = context.getEntry(Registries.BLOCK, arguments.getIdentifier("block"), "block");
        Block block = holder.value();
        BlockState state = block.defaultBlockState();

        JsonObject properties = new JsonObject();

        for (Property<?> property : block.getStateDefinition().getProperties()) {
            properties.add(property.getName(), getValues(property));
        }

        // The lowest tier not in its incorrect_for_<tier>_tool tag; null when a hand works too.
        String minToolTier = null;

        if (state.requiresCorrectToolForDrops()) {
            minToolTier = "none";

            for (String tier : List.of("netherite", "diamond", "iron", "stone", "wooden")) {
                if (!state.is(TagKey.create(Registries.BLOCK, ResourceLocation.withDefaultNamespace("incorrect_for_" + tier + "_tool")))) {
                    minToolTier = tier;
                }
            }
        }

        JsonObject json = new JsonObject();

        json.addProperty("id", holder.key().location().toString());
        json.addProperty("name", block.getName().getString());
        json.add("properties", properties);
        json.addProperty("default_state", BlockStateParser.serialize(state));
        json.addProperty("hardness", block.defaultDestroyTime());
        json.addProperty("blast_resistance", block.getExplosionResistance());
        json.addProperty("light_emission", state.getLightEmission());
        json.add("mineable_with", Stream.of("axe", "hoe", "pickaxe", "shovel")
                .filter(tool -> state.is(TagKey.create(Registries.BLOCK, ResourceLocation.withDefaultNamespace("mineable/" + tool))))
                .collect(JsonArray::new, JsonArray::add, JsonArray::addAll));
        json.addProperty("min_tool_tier", minToolTier);
        json.addProperty("loot_table", block.getLootTable() == BuiltInLootTables.EMPTY ? null : block.getLootTable().location().toString());
        json.addProperty("item", block.asItem() == Items.AIR ? null : BuiltInRegistries.ITEM.getKey(block.asItem()).toString());

        return ExecutionResult.success(json);
    }

    private static <T extends Comparable<T>> JsonArray getValues(Property<T> property) {
        JsonArray values = new JsonArray();
        property.getPossibleValues().forEach(value -> values.add(property.getName(value)));
        return values;
    }
}
