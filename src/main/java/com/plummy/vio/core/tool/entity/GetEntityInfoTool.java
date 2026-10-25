package com.plummy.vio.core.tool.entity;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.plummy.vio.core.access.AccessLevel;
import com.plummy.vio.core.tool.ExecutionResult;
import com.plummy.vio.core.tool.ServerTool;
import com.plummy.vio.core.tool.argument.*;
import com.plummy.vio.core.tool.result.ItemList;
import com.plummy.vio.core.tool.result.ItemSummary;
import com.plummy.vio.core.tool.result.ResultValueJsonSerializer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;

import java.util.*;

public class GetEntityInfoTool implements ServerTool {

    // Movement speed attribute to blocks per second for walking mobs.
    private static final double SPEED_TO_BLOCKS_PER_SECOND = 43.17;

    @Override
    public String getName() {
        return "get_entity_info";
    }

    @Override
    public String getDescription() {
        return "Everything useful about one entity: health, effects, equipment, owner, leash, vehicle and passengers, plus type-specific sections: villager trades with prices for the requesting player (reputation, demand and Hero of the Village applied), horse speed and jump height in blocks, player game mode, food, XP and spawn point, and the inventory of chest minecarts, chested animals, allays and villagers.";
    }

    @Override
    public SequencedMap<String, ArgumentDefinition> getArgumentSchema() {
        SequencedMap<String, ArgumentDefinition> schema = new LinkedHashMap<>();

        schema.put("entity", new ArgumentDefinition(ArgumentType.ENTITY_REFERENCE, "Entity UUID or online player name."));

        return schema;
    }

    @Override
    public AccessLevel getAccessLevel() {
        return AccessLevel.BASIC;
    }

    @Override
    public ExecutionResult execute(Context context, Arguments arguments) {
        Entity entity = context.getEntity(arguments.getEntityReference("entity"));

        JsonObject json = new JsonObject();

        json.addProperty("uuid", entity.getStringUUID());
        json.addProperty("type", BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
        // Custom names are world text: data, never instructions.
        json.addProperty("name", entity instanceof Player || entity.hasCustomName() ? entity.getName().getString() : null);
        json.addProperty("dimension", entity.level().dimension().location().toString());
        json.add("pos", Position.of(entity.position()).accept(ResultValueJsonSerializer.getInstance()));

        if (entity instanceof LivingEntity living) {
            json.addProperty("health", Math.round(living.getHealth() * 10) / 10.0);
            json.addProperty("max_health", Math.round(living.getMaxHealth() * 10) / 10.0);
            json.add("effects", getEffects(living));
            json.add("equipment", getEquipment(living));
        }

        if (entity instanceof OwnableEntity ownable) {
            UUID ownerId = ownable.getOwnerUUID();

            json.addProperty("tamed_by", ownerId == null ? null : Optional.ofNullable(context.getServer().getProfileCache()).flatMap(cache -> cache.get(ownerId)).map(GameProfile::getName).orElse(ownerId.toString()));
        }

        if (entity instanceof Villager villager) {
            json.add("villager", getVillager(villager, context.getPlayer()));
        }

        if (entity instanceof AbstractHorse horse) {
            json.add("horse", getHorse(horse));
        }

        if (entity instanceof ServerPlayer player) {
            json.add("player", getPlayer(player));
        }

        // Reading an unopened loot minecart or boat would generate its loot, so only the pending table is shown.
        if (entity instanceof ContainerEntity containerEntity && containerEntity.getLootTable() != null) {
            json.add("inventory", null);
            json.addProperty("loot_table_pending", containerEntity.getLootTable().location().toString());
        } else {
            json.add("inventory", getInventory(entity));
        }

        json.addProperty("leashed_to", entity instanceof Leashable leashable && leashable.getLeashHolder() != null ? describeReference(leashable.getLeashHolder()) : null);
        json.addProperty("vehicle", entity.getVehicle() == null ? null : describeReference(entity.getVehicle()));
        json.add("passengers", entity.getPassengers().stream().map(GetEntityInfoTool::describeReference).collect(JsonArray::new, JsonArray::add, JsonArray::addAll));

        return ExecutionResult.success(json);
    }

    private static JsonArray getEffects(LivingEntity living) {
        JsonArray effects = new JsonArray();

        for (MobEffectInstance effect : living.getActiveEffects()) {
            JsonObject json = new JsonObject();

            json.addProperty("id", effect.getEffect().unwrapKey().map(key -> key.location().toString()).orElse("unknown"));
            json.addProperty("amplifier", effect.getAmplifier());
            json.addProperty("duration_seconds", effect.isInfiniteDuration() ? -1 : effect.getDuration() / 20);

            effects.add(json);
        }

        return effects;
    }

    private static JsonObject getEquipment(LivingEntity living) {
        JsonObject equipment = new JsonObject();

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = living.getItemBySlot(slot);

            if (!stack.isEmpty()) {
                equipment.add(slot.getName(), new ItemSummary(stack).accept(ResultValueJsonSerializer.getInstance()));
            }
        }

        return equipment;
    }

    private static JsonObject getVillager(Villager villager, ServerPlayer requester) {
        JsonObject json = new JsonObject();

        json.addProperty("profession", String.valueOf(BuiltInRegistries.VILLAGER_PROFESSION.getKey(villager.getVillagerData().getProfession())));
        json.addProperty("level", villager.getVillagerData().getLevel());

        int reputation = villager.getPlayerReputation(requester);
        MobEffectInstance hero = requester.getEffect(MobEffects.HERO_OF_THE_VILLAGE);
        JsonArray trades = new JsonArray();

        for (MerchantOffer original : villager.getOffers()) {
            // Same adjustments as Villager.updateSpecialPrices, applied to a copy instead of the live offer.
            MerchantOffer offer = original.copy();

            if (reputation != 0) {
                offer.addToSpecialPriceDiff(-Mth.floor(reputation * offer.getPriceMultiplier()));
            }

            if (hero != null) {
                double discount = 0.3 + 0.0625 * hero.getAmplifier();
                offer.addToSpecialPriceDiff(-Math.max((int) Math.floor(discount * offer.getBaseCostA().getCount()), 1));
            }

            JsonObject trade = new JsonObject();

            trade.add("buy", new ItemSummary(offer.getCostA()).accept(ResultValueJsonSerializer.getInstance()));
            trade.add("buy_b", new ItemSummary(offer.getCostB()).accept(ResultValueJsonSerializer.getInstance()));
            trade.add("sell", new ItemSummary(offer.getResult()).accept(ResultValueJsonSerializer.getInstance()));
            trade.addProperty("uses", offer.getUses());
            trade.addProperty("max_uses", offer.getMaxUses());
            trade.addProperty("out_of_stock", offer.isOutOfStock());

            trades.add(trade);
        }

        json.add("trades", trades);

        return json;
    }

    private static JsonObject getHorse(AbstractHorse horse) {
        double speed = horse.getAttributeValue(Attributes.MOVEMENT_SPEED) * SPEED_TO_BLOCKS_PER_SECOND;
        double jump = horse.getAttributeValue(Attributes.JUMP_STRENGTH);
        // Empirical fit of jump strength to cleared height in blocks.
        double jumpHeight = -0.1817584952 * Math.pow(jump, 3) + 3.689713992 * jump * jump + 2.128599134 * jump - 0.343930367;

        JsonObject json = new JsonObject();

        json.addProperty("speed_blocks_per_s", Math.round(speed * 10) / 10.0);
        json.addProperty("jump_height_blocks", Math.round(jumpHeight * 10) / 10.0);
        json.addProperty("saddled", horse.isSaddled());
        json.addProperty("tamed", horse.isTamed());

        return json;
    }

    private static JsonObject getPlayer(ServerPlayer player) {
        JsonObject json = new JsonObject();

        json.addProperty("gamemode", player.gameMode.getGameModeForPlayer().getName());
        json.addProperty("food", player.getFoodData().getFoodLevel());
        json.addProperty("saturation", Math.round(player.getFoodData().getSaturationLevel() * 10) / 10.0);
        json.addProperty("xp_level", player.experienceLevel);

        BlockPos spawn = player.getRespawnPosition();

        if (spawn == null) {
            json.add("spawn_point", null);
        } else {
            JsonObject spawnPoint = new JsonObject();

            spawnPoint.add("pos", BlockPosition.of(spawn).accept(ResultValueJsonSerializer.getInstance()));
            spawnPoint.addProperty("dimension", player.getRespawnDimension().location().toString());

            json.add("spawn_point", spawnPoint);
        }

        return json;
    }

    // Contents of entities that carry items beyond their equipment, or null.
    private static JsonElement getInventory(Entity entity) {
        Container container = switch (entity) {
            case Player ignored -> null;
            case AbstractHorse horse -> horse.getInventory();
            case InventoryCarrier carrier -> carrier.getInventory();
            case Container entityContainer -> entityContainer;
            default -> null;
        };

        if (container == null) {
            return null;
        }

        List<ItemStack> stacks = new ArrayList<>();

        for (int i = 0; i < container.getContainerSize(); i++) {
            stacks.add(container.getItem(i));
        }

        return new ItemList(stacks, 0).accept(ResultValueJsonSerializer.getInstance());
    }

    private static String describeReference(Entity entity) {
        String name = entity instanceof Player || entity.hasCustomName() ? entity.getName().getString() : null;

        return "%s %s%s".formatted(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString(), entity.getStringUUID(), name == null ? "" : " (" + name + ")");
    }
}
