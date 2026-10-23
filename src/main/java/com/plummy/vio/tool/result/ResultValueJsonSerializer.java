package com.plummy.vio.tool.result;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.plummy.vio.tool.argument.BlockPosition;
import com.plummy.vio.tool.argument.Position;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.*;

// Compact on purpose: units go in key names, positions are [x, y, z] arrays.
public class ResultValueJsonSerializer implements ResultValueVisitor<JsonElement> {

    private static final ResultValueJsonSerializer INSTANCE = new ResultValueJsonSerializer();

    // Shown in their own fields, so not repeated in extra_components.
    private static final Set<DataComponentType<?>> SUMMARIZED_COMPONENTS = Set.of(
            DataComponents.DAMAGE,
            DataComponents.ENCHANTMENTS,
            DataComponents.STORED_ENCHANTMENTS,
            DataComponents.CUSTOM_NAME
    );

    public static ResultValueJsonSerializer getInstance() {
        return INSTANCE;
    }

    private ResultValueJsonSerializer() {
    }

    // To one decimal, which is enough for entity positions.
    @Override
    public JsonElement visitPosition(Position value) {
        JsonArray json = new JsonArray();

        json.add(Math.round(value.getX() * 10) / 10.0);
        json.add(Math.round(value.getY() * 10) / 10.0);
        json.add(Math.round(value.getZ() * 10) / 10.0);

        return json;
    }

    @Override
    public JsonElement visitBlockPosition(BlockPosition value) {
        JsonArray json = new JsonArray();

        json.add(value.getX());
        json.add(value.getY());
        json.add(value.getZ());

        return json;
    }

    @Override
    public JsonElement visitItemSummary(ItemSummary value) {
        ItemStack stack = value.getStack();

        if (stack.isEmpty()) {
            return JsonNull.INSTANCE;
        }

        JsonObject json = new JsonObject();

        json.addProperty("item", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        json.addProperty("count", stack.getCount());

        if (stack.isDamageableItem()) {
            json.addProperty("damage", "%d/%d".formatted(stack.getDamageValue(), stack.getMaxDamage()));
        }

        // Keyed by the component's own name: enchantments, stored_enchantments.
        for (DataComponentType<ItemEnchantments> type : List.of(DataComponents.ENCHANTMENTS, DataComponents.STORED_ENCHANTMENTS)) {
            ItemEnchantments enchantments = stack.getOrDefault(type, ItemEnchantments.EMPTY);

            if (!enchantments.isEmpty()) {
                Map<String, Integer> levels = new TreeMap<>();

                for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
                    levels.put(entry.getKey().unwrapKey().map(key -> key.location().toString()).orElse("unknown"), entry.getIntValue());
                }

                JsonObject levelsJson = new JsonObject();
                levels.forEach(levelsJson::addProperty);
                json.add(BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type).getPath(), levelsJson);
            }
        }

        // A player-chosen name is world text: data, never instructions.
        json.addProperty("name", stack.has(DataComponents.CUSTOM_NAME) ? stack.getHoverName().getString() : null);

        // Ids only, so the model knows when get_entity_nbt is worth calling.
        List<String> extra = new ArrayList<>();

        for (Map.Entry<DataComponentType<?>, Optional<?>> entry : stack.getComponentsPatch().entrySet()) {
            if (!SUMMARIZED_COMPONENTS.contains(entry.getKey())) {
                extra.add(String.valueOf(BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(entry.getKey())));
            }
        }

        json.add("extra_components", extra.stream().sorted().collect(JsonArray::new, JsonArray::add, JsonArray::addAll));

        return json;
    }

    @Override
    public JsonElement visitItemList(ItemList value) {
        JsonArray json = new JsonArray();
        List<ItemStack> stacks = value.getStacks();

        for (int i = 0; i < stacks.size(); i++) {
            if (stacks.get(i).isEmpty()) {
                continue;
            }

            JsonObject item = new JsonObject();

            item.addProperty("slot", value.getFirstSlot() + i);
            visitItemSummary(new ItemSummary(stacks.get(i))).getAsJsonObject().entrySet().forEach(entry -> item.add(entry.getKey(), entry.getValue()));

            json.add(item);
        }

        return json;
    }
}
