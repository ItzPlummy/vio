package com.plummy.vio.core.tool.catalog.player;

import com.google.gson.JsonObject;
import com.plummy.vio.core.access.AccessLevel;
import com.plummy.vio.core.tool.executor.ExecutionResult;
import com.plummy.vio.core.tool.ServerTool;
import com.plummy.vio.core.tool.value.argument.ArgumentDefinition;
import com.plummy.vio.core.tool.value.argument.ArgumentType;
import com.plummy.vio.core.tool.value.argument.Arguments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.LinkedHashMap;
import java.util.SequencedMap;

public class OpenContainerTool implements ServerTool {

    @Override
    public String getName() {
        return "open_container";
    }

    @Override
    public String getDescription() {
        return "Open a storage container at a position for the requesting player remotely: chests (double chests whole), barrels, shulker boxes, hoppers, dispensers, droppers. Pending loot is generated, as if opened in person.";
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
        return AccessLevel.FULL;
    }

    @Override
    public ExecutionResult execute(Context context, Arguments arguments) {
        ServerPlayer player = context.getPlayer();
        ServerLevel level = context.getLevel(arguments.getDimension("dimension"));
        BlockPos pos = arguments.getBlockPosition("position").toBlockPos();

        context.requireLoaded(level, pos);

        BlockState state = level.getBlockState(pos);
        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (!(blockEntity instanceof Container container)) {
            return ExecutionResult.invalidArguments("%s at %s %s %s (%s) isn't a container".formatted(BuiltInRegistries.BLOCK.getKey(state.getBlock()), pos.getX(), pos.getY(), pos.getZ(), level.dimension().location()));
        }

        // Opening generates pending loot, just as opening it in person would.
        if (blockEntity instanceof RandomizableContainer randomizable) {
            randomizable.unpackLootTable(player);
        }

        if (state.getBlock() instanceof ChestBlock chest && level.isLoaded(pos.relative(ChestBlock.getConnectedDirection(state)))) {
            Container combined = ChestBlock.getContainer(chest, state, level, pos, true);

            if (combined != null) {
                container = combined;
            }
        }

        MenuFactory factory = getFactory(container, state, blockEntity);

        if (factory == null) {
            return ExecutionResult.invalidArguments("%s can't be opened remotely; only storage containers (chests, barrels, shulker boxes, hoppers, dispensers, droppers) can".formatted(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString()));
        }

        player.openMenu(new SimpleMenuProvider(factory::create, state.getBlock().getName()));

        JsonObject json = new JsonObject();

        json.addProperty("opened", BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
        json.addProperty("for", player.getGameProfile().getName());

        return ExecutionResult.success(json);
    }

    // A menu over the real container that stays open at any distance while the block entity exists.
    private static MenuFactory getFactory(Container container, BlockState state, BlockEntity blockEntity) {
        int size = container.getContainerSize();

        if (state.getBlock() instanceof ShulkerBoxBlock) {
            return (id, inventory, owner) -> new ShulkerBoxMenu(id, inventory, container) {
                @Override
                public boolean stillValid(Player viewer) {
                    return !blockEntity.isRemoved();
                }
            };
        }

        if (size == 5) {
            return (id, inventory, owner) -> new HopperMenu(id, inventory, container) {
                @Override
                public boolean stillValid(Player viewer) {
                    return !blockEntity.isRemoved();
                }
            };
        }

        if (size == 9) {
            return (id, inventory, owner) -> new DispenserMenu(id, inventory, container) {
                @Override
                public boolean stillValid(Player viewer) {
                    return !blockEntity.isRemoved();
                }
            };
        }

        if (size % 9 != 0 || size > 54) {
            return null;
        }

        int rows = size / 9;
        MenuType<?> type = switch (rows) {
            case 1 -> MenuType.GENERIC_9x1;
            case 2 -> MenuType.GENERIC_9x2;
            case 3 -> MenuType.GENERIC_9x3;
            case 4 -> MenuType.GENERIC_9x4;
            case 5 -> MenuType.GENERIC_9x5;
            default -> MenuType.GENERIC_9x6;
        };

        return (id, inventory, owner) -> new ChestMenu(type, id, inventory, container, rows) {
            @Override
            public boolean stillValid(Player viewer) {
                return !blockEntity.isRemoved();
            }
        };
    }

    private interface MenuFactory {

        AbstractContainerMenu create(int id, Inventory inventory, Player owner);
    }
}
