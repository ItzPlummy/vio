package com.plummy.vio.tool.player;

import com.google.gson.JsonObject;
import com.plummy.vio.access.AccessLevel;
import com.plummy.vio.tool.ExecutionResult;
import com.plummy.vio.tool.ServerTool;
import com.plummy.vio.tool.argument.ArgumentDefinition;
import com.plummy.vio.tool.argument.ArgumentType;
import com.plummy.vio.tool.argument.Arguments;
import com.plummy.vio.tool.result.ItemList;
import com.plummy.vio.tool.result.ItemSummary;
import com.plummy.vio.tool.result.ResultValueJsonSerializer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;

public class GetInventoryTool implements ServerTool {

    @Override
    public String getName() {
        return "get_inventory";
    }

    @Override
    public String getDescription() {
        return "An online player's inventory: hotbar (slots 0-8), main inventory (9-35), armor, offhand and optionally the ender chest. Items show count, damage, enchantments, custom name and the ids of other non-default components (read their values with get_entity_nbt).";
    }

    @Override
    public SequencedMap<String, ArgumentDefinition> getArgumentSchema() {
        SequencedMap<String, ArgumentDefinition> schema = new LinkedHashMap<>();

        schema.put("player", new ArgumentDefinition(ArgumentType.PLAYER_REFERENCE, "Online player name."));
        schema.put("include_ender_chest", new ArgumentDefinition(ArgumentType.BOOLEAN, "Whether to include the ender chest."));

        return schema;
    }

    @Override
    public AccessLevel getAccessLevel() {
        return AccessLevel.BASIC;
    }

    @Override
    public ExecutionResult execute(Context context, Arguments arguments) {
        ServerPlayer player = context.getPlayer(arguments.getPlayerReference("player"));
        boolean includeEnderChest = arguments.getBoolean("include_ender_chest");
        Inventory inventory = player.getInventory();

        List<ItemStack> hotbar = inventory.items.subList(0, Inventory.getSelectionSize());
        List<ItemStack> main = inventory.items.subList(Inventory.getSelectionSize(), inventory.items.size());

        JsonObject armor = new JsonObject();
        armor.add("head", new ItemSummary(player.getItemBySlot(EquipmentSlot.HEAD)).accept(ResultValueJsonSerializer.getInstance()));
        armor.add("chest", new ItemSummary(player.getItemBySlot(EquipmentSlot.CHEST)).accept(ResultValueJsonSerializer.getInstance()));
        armor.add("legs", new ItemSummary(player.getItemBySlot(EquipmentSlot.LEGS)).accept(ResultValueJsonSerializer.getInstance()));
        armor.add("feet", new ItemSummary(player.getItemBySlot(EquipmentSlot.FEET)).accept(ResultValueJsonSerializer.getInstance()));

        JsonObject json = new JsonObject();

        json.addProperty("player", player.getGameProfile().getName());
        json.addProperty("selected_slot", inventory.selected);
        json.add("hotbar", new ItemList(hotbar, 0).accept(ResultValueJsonSerializer.getInstance()));
        json.add("main", new ItemList(main, Inventory.getSelectionSize()).accept(ResultValueJsonSerializer.getInstance()));
        json.add("armor", armor);
        json.add("offhand", new ItemSummary(player.getOffhandItem()).accept(ResultValueJsonSerializer.getInstance()));

        if (includeEnderChest) {
            List<ItemStack> enderChest = new ArrayList<>();

            for (int i = 0; i < player.getEnderChestInventory().getContainerSize(); i++) {
                enderChest.add(player.getEnderChestInventory().getItem(i));
            }

            json.add("ender_chest", new ItemList(enderChest, 0).accept(ResultValueJsonSerializer.getInstance()));
        } else {
            json.add("ender_chest", null);
        }

        json.addProperty("empty_slots", (int) inventory.items.stream().filter(ItemStack::isEmpty).count());

        return ExecutionResult.success(json);
    }
}
