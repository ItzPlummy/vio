package com.plummy.vio.core.tool.catalog.player;

import com.google.gson.JsonObject;
import com.plummy.vio.core.access.AccessLevel;
import com.plummy.vio.core.tool.executor.ExecutionResult;
import com.plummy.vio.core.tool.ServerTool;
import com.plummy.vio.core.tool.value.argument.ArgumentDefinition;
import com.plummy.vio.core.tool.value.argument.Arguments;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.SequencedMap;

public class OpenMenuTool implements ServerTool {

    @Override
    public String getName() {
        return "open_menu";
    }

    @Override
    public String getDescription() {
        return "Open a menu for the requesting player remotely: their ender chest, or a crafting table, anvil, stonecutter, smithing table, loom, grindstone or cartography table anywhere. Containers at a position have their own tool (open_container). The enchanting table isn't offered: its power depends on nearby bookshelves.";
    }

    @Override
    public SequencedMap<String, ArgumentDefinition> getArgumentSchema() {
        SequencedMap<String, ArgumentDefinition> schema = new LinkedHashMap<>();

        schema.put("status", ArgumentDefinition.status());
        schema.put("menu", ArgumentDefinition.enumeration(Menu.class, "Which menu."));

        return schema;
    }

    @Override
    public AccessLevel getAccessLevel() {
        return AccessLevel.FULL;
    }

    @Override
    public ExecutionResult execute(Context context, Arguments arguments) {
        Menu menu = arguments.getEnum("menu", Menu.class);
        ServerPlayer player = context.getPlayer();
        ContainerLevelAccess here = ContainerLevelAccess.create(player.level(), player.blockPosition());
        String opened;

        switch (menu) {
            case ENDER_CHEST -> {
                PlayerEnderChestContainer enderChest = player.getEnderChestInventory();
                player.openMenu(new SimpleMenuProvider((id, inventory, owner) -> ChestMenu.threeRows(id, inventory, enderChest), Component.translatable("container.enderchest")));
                opened = "minecraft:ender_chest";
            }
            case CRAFTING_TABLE -> {
                player.openMenu(new SimpleMenuProvider((id, inventory, owner) -> new CraftingMenu(id, inventory, here) {

                    @Override
                    public boolean stillValid(@NotNull Player viewer) {
                        return true;
                    }
                }, Component.translatable("container.crafting")));
                opened = "minecraft:crafting_table";
            }
            case ANVIL -> {
                player.openMenu(new SimpleMenuProvider((id, inventory, owner) -> new AnvilMenu(id, inventory, here) {

                    @Override
                    public boolean stillValid(@NotNull Player viewer) {
                        return true;
                    }
                }, Component.translatable("container.repair")));
                opened = "minecraft:anvil";
            }
            case STONECUTTER -> {
                player.openMenu(new SimpleMenuProvider((id, inventory, owner) -> new StonecutterMenu(id, inventory, here) {

                    @Override
                    public boolean stillValid(@NotNull Player viewer) {
                        return true;
                    }
                }, Component.translatable("container.stonecutter")));
                opened = "minecraft:stonecutter";
            }
            case SMITHING_TABLE -> {
                player.openMenu(new SimpleMenuProvider((id, inventory, owner) -> new SmithingMenu(id, inventory, here) {

                    @Override
                    public boolean stillValid(@NotNull Player viewer) {
                        return true;
                    }
                }, Component.translatable("container.upgrade")));
                opened = "minecraft:smithing_table";
            }
            case LOOM -> {
                player.openMenu(new SimpleMenuProvider((id, inventory, owner) -> new LoomMenu(id, inventory, here) {

                    @Override
                    public boolean stillValid(@NotNull Player viewer) {
                        return true;
                    }
                }, Component.translatable("container.loom")));
                opened = "minecraft:loom";
            }
            case GRINDSTONE -> {
                player.openMenu(new SimpleMenuProvider((id, inventory, owner) -> new GrindstoneMenu(id, inventory, here) {

                    @Override
                    public boolean stillValid(@NotNull Player viewer) {
                        return true;
                    }
                }, Component.translatable("container.grindstone_title")));
                opened = "minecraft:grindstone";
            }
            case CARTOGRAPHY_TABLE -> {
                player.openMenu(new SimpleMenuProvider((id, inventory, owner) -> new CartographyTableMenu(id, inventory, here) {

                    @Override
                    public boolean stillValid(@NotNull Player viewer) {
                        return true;
                    }
                }, Component.translatable("container.cartography_table")));
                opened = "minecraft:cartography_table";
            }
            default -> throw new IllegalStateException("Unhandled menu %s".formatted(menu));
        }

        JsonObject json = new JsonObject();

        json.addProperty("opened", opened);
        json.addProperty("for", player.getGameProfile().getName());

        return ExecutionResult.success(json);
    }

    private enum Menu {
        ENDER_CHEST,
        CRAFTING_TABLE,
        ANVIL,
        STONECUTTER,
        SMITHING_TABLE,
        LOOM,
        GRINDSTONE,
        CARTOGRAPHY_TABLE
    }
}
