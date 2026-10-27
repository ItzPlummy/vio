package com.plummy.vio.tool.value.result;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.plummy.vio.tool.value.catalog.*;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.*;

public class ResultValueJsonSerializer implements ResultValueVisitor<JsonElement> {

    private static final ResultValueJsonSerializer INSTANCE = new ResultValueJsonSerializer();

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
        ItemStack stack = value.getItemStack();

        if (stack.isEmpty()) {
            return JsonNull.INSTANCE;
        }

        JsonObject json = new JsonObject();

        json.addProperty("item", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        json.addProperty("count", stack.getCount());

        if (stack.isDamageableItem()) {
            json.addProperty("damage", "%s/%s".formatted(stack.getDamageValue(), stack.getMaxDamage()));
        }

        for (DataComponentType<ItemEnchantments> type : List.of(DataComponents.ENCHANTMENTS, DataComponents.STORED_ENCHANTMENTS)) {
            ItemEnchantments enchantments = stack.getOrDefault(type, ItemEnchantments.EMPTY);

            if (!enchantments.isEmpty()) {
                Map<String, Integer> levels = new TreeMap<>();

                for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
                    levels.put(entry.getKey().unwrapKey().map(key -> key.location().toString()).orElse("unknown"), entry.getIntValue());
                }

                JsonObject levelsJson = new JsonObject();
                levels.forEach(levelsJson::addProperty);
                json.add(Objects.requireNonNull(BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type)).getPath(), levelsJson);
            }
        }

        json.addProperty("name", stack.has(DataComponents.CUSTOM_NAME) ? stack.getHoverName().getString() : null);

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

        for (Map.Entry<Integer, ItemStack> entry : value.getItemStacks().entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }

            JsonObject item = new JsonObject();

            item.addProperty("slot", entry.getKey());
            visitItemSummary(new ItemSummary(entry.getValue())).getAsJsonObject().entrySet().forEach(property -> item.add(property.getKey(), property.getValue()));

            json.add(item);
        }

        return json;
    }

    @Override
    public JsonElement visitPage(Page value) {
        JsonObject json = new JsonObject();

        json.addProperty("offset", value.getOffset());
        json.addProperty("total", value.getTotal());
        json.addProperty("next_offset", value.getNextOffset());

        return json;
    }
}
