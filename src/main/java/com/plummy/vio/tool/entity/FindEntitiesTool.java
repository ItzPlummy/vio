package com.plummy.vio.tool.entity;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.plummy.vio.access.AccessLevel;
import com.plummy.vio.tool.ExecutionResult;
import com.plummy.vio.tool.ResultException;
import com.plummy.vio.tool.ServerTool;
import com.plummy.vio.tool.argument.ArgumentDefinition;
import com.plummy.vio.tool.argument.ArgumentType;
import com.plummy.vio.tool.argument.Arguments;
import com.plummy.vio.tool.argument.Identifier;
import com.plummy.vio.tool.argument.Page;
import com.plummy.vio.tool.argument.Position;
import com.plummy.vio.tool.result.ResultValueJsonSerializer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.function.Predicate;

public class FindEntitiesTool implements ServerTool {

    private static final int MAX_LIMIT = 100;

    @Override
    public String getName() {
        return "find_entities";
    }

    @Override
    public String getDescription() {
        return "Entities near a point, nearest first, with UUIDs for other tools. Entries carry per-type extras: item entities show item and count, tamed mobs tamed_by, villagers profession. Only loaded entities are found.";
    }

    @Override
    public SequencedMap<String, ArgumentDefinition> getArgumentSchema() {
        SequencedMap<String, ArgumentDefinition> schema = new LinkedHashMap<>();

        schema.put("entity_type", new ArgumentDefinition(ArgumentType.IDENTIFIER, "Entity type id or #tag, e.g. minecraft:cow or #minecraft:skeletons; \"\" = any."));
        schema.put("name", new ArgumentDefinition(ArgumentType.STRING, "Case-insensitive substring of the custom name or player name; \"\" = any."));
        schema.put("center", new ArgumentDefinition(ArgumentType.BLOCK_POSITION, "Search center [x, y, z]."));
        schema.put("dimension", new ArgumentDefinition(ArgumentType.DIMENSION, "Dimension id."));
        schema.put("radius_blocks", new ArgumentDefinition(ArgumentType.INTEGER, "Search radius, at most 256 (server-configurable)."));
        schema.put("include_players", new ArgumentDefinition(ArgumentType.BOOLEAN, "Whether players are included."));
        schema.put("limit", new ArgumentDefinition(ArgumentType.INTEGER, "Page size, 1 to 100; 20 is a good default."));
        schema.put("offset", new ArgumentDefinition(ArgumentType.INTEGER, "Entries to skip; 0 for the first page."));

        return schema;
    }

    @Override
    public AccessLevel getAccessLevel() {
        return AccessLevel.BASIC;
    }

    @Override
    public ExecutionResult execute(Context context, Arguments arguments) {
        ServerLevel level = context.getLevel(arguments.getDimension("dimension"));
        BlockPos pos = arguments.getBlockPosition("center").toBlockPos();
        int radius = arguments.getInt("radius_blocks", 1, 256);
        Predicate<Entity> typeFilter = getTypeFilter(context, arguments.isEmpty("entity_type") ? null : arguments.getIdentifier("entity_type"));
        String name = arguments.getOptionalString("name");
        boolean includePlayers = arguments.getBoolean("include_players");
        Page page = arguments.getPage(MAX_LIMIT);

        context.requireLoaded(level, pos);

        Vec3 center = Vec3.atCenterOf(pos);
        String nameQuery = name == null ? null : name.toLowerCase(Locale.ROOT);

        List<Entity> entities = new ArrayList<>(level.getEntities((Entity) null, AABB.ofSize(center, radius * 2, radius * 2, radius * 2), entity -> {
            if (entity.position().distanceTo(center) > radius || !typeFilter.test(entity)) {
                return false;
            }

            if (entity instanceof Player && !includePlayers) {
                return false;
            }

            if (nameQuery == null) {
                return true;
            }

            String entityName = entity instanceof Player || entity.hasCustomName() ? entity.getName().getString() : null;

            return entityName != null && entityName.toLowerCase(Locale.ROOT).contains(nameQuery);
        }));

        entities.sort(Comparator.<Entity>comparingDouble(entity -> entity.position().distanceToSqr(center)).thenComparing(Entity::getUUID));

        JsonArray results = new JsonArray();

        // Short entries; per-type extras keep them short: items show item + count, tamed mobs tamed_by, villagers profession.
        for (Entity entity : page.apply(entities)) {
            JsonObject result = new JsonObject();

            result.addProperty("uuid", entity.getStringUUID());
            result.addProperty("type", BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
            result.addProperty("name", entity instanceof Player || entity.hasCustomName() ? entity.getName().getString() : null);
            result.add("pos", Position.of(entity.position()).accept(ResultValueJsonSerializer.getInstance()));
            result.addProperty("distance_blocks", Math.round(entity.position().distanceTo(center) * 10) / 10.0);

            if (entity instanceof ItemEntity item) {
                result.addProperty("item", BuiltInRegistries.ITEM.getKey(item.getItem().getItem()).toString());
                result.addProperty("count", item.getItem().getCount());
            }

            if (entity instanceof LivingEntity living) {
                result.addProperty("health", Math.round(living.getHealth() * 10) / 10.0);
                result.addProperty("max_health", Math.round(living.getMaxHealth() * 10) / 10.0);
            }

            if (entity instanceof OwnableEntity ownable && ownable.getOwnerUUID() != null) {
                result.addProperty("tamed_by", Optional.ofNullable(context.getServer().getProfileCache()).flatMap(cache -> cache.get(ownable.getOwnerUUID())).map(GameProfile::getName).orElse(ownable.getOwnerUUID().toString()));
            }

            if (entity instanceof Villager villager) {
                result.addProperty("profession", String.valueOf(BuiltInRegistries.VILLAGER_PROFESSION.getKey(villager.getVillagerData().getProfession())));
            }

            results.add(result);
        }

        JsonObject json = new JsonObject();

        json.add("entities", results);
        page.write(json, entities.size());

        return ExecutionResult.success(json);
    }

    private static Predicate<Entity> getTypeFilter(Context context, Identifier entityType) {
        if (entityType == null) {
            return entity -> true;
        }

        if (entityType.isTag()) {
            TagKey<EntityType<?>> tag = TagKey.create(Registries.ENTITY_TYPE, entityType.getId());

            if (BuiltInRegistries.ENTITY_TYPE.getTag(tag).isEmpty()) {
                throw ResultException.invalidArguments("Unknown entity type tag \"#%s\".".formatted(tag.location()));
            }

            return entity -> entity.getType().is(tag);
        }

        EntityType<?> type = context.getEntry(BuiltInRegistries.ENTITY_TYPE, entityType, "entity type").value();
        return entity -> entity.getType() == type;
    }
}
