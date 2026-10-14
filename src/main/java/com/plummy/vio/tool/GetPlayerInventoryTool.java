package com.plummy.vio.tool;

import com.plummy.vio.access.AccessLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;

public class GetPlayerInventoryTool implements Tool {

    @Override
    public String getName() {
        return "get_player_inventory";
    }

    @Override
    public String getDescription() {
        return "List basic information about the player inventory.";
    }

    @Override
    public SequencedMap<String, ArgumentDefinition> getArgumentSchema() {
        SequencedMap<String, ArgumentDefinition> arguments = new LinkedHashMap<>();

        arguments.put(
                "name",
                new ArgumentDefinition(
                        ArgumentType.STRING,
                        "Nickname of the player whose inventory is listed."
                )
        );

        return arguments;
    }

    @Override
    public AccessLevel getAccessLevel() {
        return AccessLevel.BASIC;
    }

    @Override
    public ExecutionResult execute(Context context, Arguments arguments) {
        String name = arguments.getString("name");

        ServerPlayer player = context.getServer().getPlayerList().getPlayerByName(name);

        if (player == null) {
            return new ExecutionResult(ExecutionStatus.FAILURE, "Player not found.");
        }

        ItemStack mainHandItem = player.getMainHandItem();
        ItemStack offHandItem = player.getOffhandItem();
        List<ItemStack> armor = player.getInventory().armor;
        List<ItemStack> inventory = player.getInventory().items;

        List<String> armorStrings = new ArrayList<>();
        List<String> inventoryStrings = new ArrayList<>();

        for (ItemStack armorItem : armor) {
            if (armorItem == null || armorItem.isEmpty()) {
                continue;
            }

            armorStrings.add(GetPlayerInventoryTool.getItemStackAsString(armorItem));
        }

        for (ItemStack inventoryItem : inventory) {
            if (inventoryItem == null || inventoryItem.isEmpty()) {
                continue;
            }

            inventoryStrings.add(GetPlayerInventoryTool.getItemStackAsString(inventoryItem));
        }

        String result = "Main hand item: %s, ".formatted(getItemStackAsString(mainHandItem)) +
                "Off hand item: %s, ".formatted(getItemStackAsString(offHandItem)) +
                "Armor: [%s], ".formatted(String.join(", ", armorStrings)) +
                "Inventory: [%s].".formatted(String.join(", ", inventoryStrings));

        return new ExecutionResult(ExecutionStatus.SUCCESS, result);
    }

    private static String getItemStackAsString(ItemStack itemStack) {
        return "(" + itemStack.getCount() + " " + itemStack.getItem().getName(itemStack).getString(64) + ")";
    }
}
