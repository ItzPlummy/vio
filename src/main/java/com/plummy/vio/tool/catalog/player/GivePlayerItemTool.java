package com.plummy.vio.tool.catalog.player;

import com.google.gson.JsonObject;
import com.plummy.vio.access.AccessLevel;
import com.plummy.vio.tool.ServerTool;
import com.plummy.vio.tool.executor.ExecutionResult;
import com.plummy.vio.tool.value.argument.ArgumentDefinition;
import com.plummy.vio.tool.value.argument.ArgumentType;
import com.plummy.vio.tool.value.argument.Arguments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.SequencedMap;

public class GivePlayerItemTool implements ServerTool {

    private static final int MAX_STACKS = 100;

    @Override
    public String getName() {
        return "give_player_item";
    }

    @Override
    public String getDescription() {
        return "Give items to an online player like /give, with 1.21.1 data components. Stacks are split by the item's max stack size; what doesn't fit is dropped at the player's feet for them only to pick up. Components go inside [...] after the id, e.g. minecraft:diamond_sword[enchantments={levels:{'minecraft:sharpness':5}},custom_name='\"Excalibur\"']. Arbitrary NBT goes in custom_data={...}. The pre-1.20.5 NBT format (display:, Enchantments:, tag:) no longer works.";
    }

    @Override
    public SequencedMap<String, ArgumentDefinition> getArgumentSchema() {
        SequencedMap<String, ArgumentDefinition> schema = new LinkedHashMap<>();

        schema.put("status", ArgumentDefinition.status());
        schema.put("player", new ArgumentDefinition(ArgumentType.PLAYER_REFERENCE, "Online player name."));
        schema.put("item", new ArgumentDefinition(ArgumentType.ITEM_SPEC, "Item like /give: id with optional [components], e.g. minecraft:diamond_sword[enchantments={levels:{'minecraft:sharpness':5}}]."));
        schema.put("count", new ArgumentDefinition(ArgumentType.INTEGER, "How many, at most 100 stacks."));

        return schema;
    }

    @Override
    public AccessLevel getAccessLevel() {
        return AccessLevel.FULL;
    }

    @Override
    public ExecutionResult execute(Context context, Arguments arguments) {
        ServerPlayer player = context.getPlayer(arguments.getPlayerReference("player"));

        ItemStack template = arguments.getItemSpec("item").toItemStack(player.serverLevel());
        int maxStackSize = template.getMaxStackSize();
        int count = arguments.getInt("count", 1, maxStackSize * MAX_STACKS);

        int given = 0;
        int dropped = 0;
        int discarded = 0;
        int remaining = count;

        while (remaining > 0) {
            int size = Math.min(maxStackSize, remaining);
            ItemStack stack = template.copyWithCount(size);

            remaining -= size;
            player.getInventory().add(stack);
            given += size - stack.getCount();

            if (stack.isEmpty()) {
                continue;
            }

            ItemEntity item = player.drop(stack, false);

            if (item != null) {
                item.setNoPickUpDelay();
                item.setTarget(player.getUUID());
                dropped += stack.getCount();
            } else {
                discarded += stack.getCount();
            }
        }

        if (given > 0) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F,
                    ((player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F);
            player.containerMenu.broadcastChanges();
        }

        JsonObject json = new JsonObject();

        json.addProperty("player", player.getGameProfile().getName());
        json.addProperty("item", template.getItemHolder().unwrapKey().map(key -> key.location().toString()).orElse("unknown"));
        json.addProperty("given", given);
        json.addProperty("dropped", dropped);
        json.addProperty("discarded", discarded);

        return ExecutionResult.success(json);
    }
}
