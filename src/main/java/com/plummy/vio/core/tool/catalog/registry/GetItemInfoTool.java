package com.plummy.vio.core.tool.catalog.registry;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.plummy.vio.core.access.AccessLevel;
import com.plummy.vio.core.tool.executor.ExecutionResult;
import com.plummy.vio.core.tool.ServerTool;
import com.plummy.vio.core.tool.value.argument.ArgumentDefinition;
import com.plummy.vio.core.tool.value.argument.ArgumentType;
import com.plummy.vio.core.tool.value.argument.Arguments;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.datamaps.builtin.Compostable;
import net.neoforged.neoforge.registries.datamaps.builtin.FurnaceFuel;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.SequencedMap;

public class GetItemInfoTool implements ServerTool {

    private static final int MAX_TAGS = 20;

    @Override
    public String getName() {
        return "get_item_info";
    }

    @Override
    public String getDescription() {
        return "What an item type is and does: stack size, durability, rarity, food values, furnace fuel burn time, composting chance, the block it places, and its tags (up to 20). Fuel and compost values come from data maps, so modded items are covered.";
    }

    @Override
    public SequencedMap<String, ArgumentDefinition> getArgumentSchema() {
        SequencedMap<String, ArgumentDefinition> schema = new LinkedHashMap<>();

        schema.put("status", ArgumentDefinition.status());
        schema.put("item", new ArgumentDefinition(ArgumentType.IDENTIFIER, "Item id."));

        return schema;
    }

    @Override
    public AccessLevel getAccessLevel() {
        return AccessLevel.BASIC;
    }

    @Override
    public ExecutionResult execute(Context context, Arguments arguments) {
        Holder.Reference<Item> holder = context.getEntry(Registries.ITEM, arguments.getIdentifier("item"), "item");
        Item item = holder.value();
        ItemStack stack = new ItemStack(holder);
        FoodProperties food = stack.get(DataComponents.FOOD);
        FurnaceFuel fuel = BuiltInRegistries.ITEM.getData(NeoForgeDataMaps.FURNACE_FUELS, holder.key());
        Compostable compostable = BuiltInRegistries.ITEM.getData(NeoForgeDataMaps.COMPOSTABLES, holder.key());

        JsonObject json = new JsonObject();

        json.addProperty("id", holder.key().location().toString());
        json.addProperty("name", item.getDescription().getString());
        json.addProperty("max_stack_size", stack.getMaxStackSize());
        json.addProperty("max_damage", stack.isDamageableItem() ? stack.getMaxDamage() : null);
        json.addProperty("rarity", stack.getRarity().getSerializedName().toLowerCase(Locale.ROOT));

        if (food != null) {
            JsonObject foodJson = new JsonObject();

            foodJson.addProperty("nutrition", food.nutrition());
            foodJson.addProperty("saturation", Math.round(food.saturation() * 100) / 100.0);
            foodJson.addProperty("always_edible", food.canAlwaysEat());

            json.add("food", foodJson);
        } else {
            json.add("food", null);
        }

        json.addProperty("fuel_burn_ticks", fuel == null ? null : fuel.burnTime());
        json.addProperty("compost_chance", compostable == null ? null : Math.round(compostable.chance() * 100) / 100.0);
        json.addProperty("places_block", item instanceof BlockItem blockItem ? BuiltInRegistries.BLOCK.getKey(blockItem.getBlock()).toString() : null);
        json.add("tags", holder.tags().map(tag -> "#%s".formatted(tag.location())).sorted().limit(MAX_TAGS).collect(JsonArray::new, JsonArray::add, JsonArray::addAll));

        return ExecutionResult.success(json);
    }
}
