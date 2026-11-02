package com.plummy.vio.tool.catalog.registry;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.plummy.vio.access.AccessLevel;
import com.plummy.vio.tool.ServerTool;
import com.plummy.vio.tool.executor.ExecutionResult;
import com.plummy.vio.tool.executor.ExecutionResultException;
import com.plummy.vio.tool.value.argument.ArgumentDefinition;
import com.plummy.vio.tool.value.argument.ArgumentType;
import com.plummy.vio.tool.value.argument.Arguments;
import com.plummy.vio.tool.value.catalog.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.*;

public class GetLootTableTool implements ServerTool {

    @Override
    public String getName() {
        return "get_loot_table";
    }

    @Override
    public String getDescription() {
        return "Drop chances of a loot table, measured by simulating rolls (nothing is given to anyone). source is a loot table id (minecraft:chests/simple_dungeon) or an entity or block id, resolved to its default table. For block tables, tool sets the breaking tool, e.g. minecraft:diamond_pickaxe[enchantments={levels:{'minecraft:fortune':3}}] or silk touch. For entity tables the requesting player is the killer, so player-kill drops apply and looting comes from what they hold.";
    }

    @Override
    public SequencedMap<String, ArgumentDefinition> getArgumentSchema() {
        SequencedMap<String, ArgumentDefinition> schema = new LinkedHashMap<>();

        schema.put("status", ArgumentDefinition.status());
        schema.put("source", new ArgumentDefinition(ArgumentType.IDENTIFIER, "Loot table id, or an entity or block id."));
        schema.put("rolls", new ArgumentDefinition(ArgumentType.INTEGER, "Rolls to simulate, 1 to 1024."));
        schema.put("tool", new ArgumentDefinition(ArgumentType.ITEM_SPEC, "Breaking tool with optional [components]; empty = bare hand."));

        return schema;
    }

    @Override
    public AccessLevel getAccessLevel() {
        return AccessLevel.BASIC;
    }

    @Override
    public ExecutionResult execute(Context context, Arguments arguments) {
        MinecraftServer server = context.getServer();
        Identifier source = arguments.getIdentifier("source");
        int rolls = arguments.getInt("rolls", 1, 1024);
        ServerPlayer player = context.getPlayer();
        ServerLevel level = player.serverLevel();
        ItemStack tool = arguments.isEmpty("tool") ? ItemStack.EMPTY : arguments.getItemSpec("tool").toItemStack(level);

        Source resolved = resolve(server, level, source.getId(), source.toString());
        LootTable table = server.reloadableRegistries().getLootTable(resolved.table());
        LootContextParamSet paramSet = table.getParamSet();
        Entity entity = resolved.entity() != null ? resolved.entity() : player;

        Map<LootContextParam<?>, Object> values = new LinkedHashMap<>();
        values.put(LootContextParams.ORIGIN, player.position());
        values.put(LootContextParams.THIS_ENTITY, entity);
        values.put(LootContextParams.DAMAGE_SOURCE, level.damageSources().playerAttack(player));
        values.put(LootContextParams.ATTACKING_ENTITY, player);
        values.put(LootContextParams.DIRECT_ATTACKING_ENTITY, player);
        values.put(LootContextParams.LAST_DAMAGE_PLAYER, player);
        values.put(LootContextParams.TOOL, tool);

        if (resolved.blockState() != null) {
            values.put(LootContextParams.BLOCK_STATE, resolved.blockState());
        }

        LootParams.Builder builder = new LootParams.Builder(level);

        for (Map.Entry<LootContextParam<?>, Object> entry : values.entrySet()) {
            if (paramSet.isAllowed(entry.getKey())) {
                setParameter(builder, entry.getKey(), entry.getValue());
            }
        }

        LootParams params;

        try {
            params = builder.create(paramSet);
        } catch (IllegalArgumentException e) {
            return ExecutionResult.failure("Loot table %s needs context this tool can't provide: %s".formatted(resolved.table().location(), e.getMessage()));
        }

        Map<String, int[]> drops = new TreeMap<>();
        RandomSource random = RandomSource.create();

        for (int roll = 0; roll < rolls; roll++) {
            Map<String, Integer> rollCounts = new HashMap<>();

            for (ItemStack stack : table.getRandomItems(params, random)) {
                rollCounts.merge(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), stack.getCount(), Integer::sum);
            }

            rollCounts.forEach((item, count) -> {
                int[] stats = drops.computeIfAbsent(item, key -> new int[3]);
                stats[0]++;
                stats[1] += count;
                stats[2] = Math.max(stats[2], count);
            });
        }

        JsonArray dropsJson = new JsonArray();

        drops.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, int[]>>comparingInt(entry -> -entry.getValue()[0]).thenComparing(Map.Entry::getKey))
                .forEach(entry -> {
                    JsonObject drop = new JsonObject();

                    drop.addProperty("item", entry.getKey());
                    drop.addProperty("chance_any", Math.round((double) entry.getValue()[0] / rolls * 1000) / 1000.0);
                    drop.addProperty("avg_count", Math.round((double) entry.getValue()[1] / rolls * 1000) / 1000.0);
                    drop.addProperty("max_count", entry.getValue()[2]);

                    dropsJson.add(drop);
                });

        JsonObject json = new JsonObject();

        json.addProperty("loot_table", resolved.table().location().toString());
        json.addProperty("rolls", rolls);
        json.add("drops", dropsJson);
        json.addProperty("note", "simulated");

        return ExecutionResult.success(json);
    }

    private static Source resolve(MinecraftServer server, ServerLevel level, ResourceLocation id, String source) {
        Collection<ResourceLocation> tables = server.reloadableRegistries().getKeys(Registries.LOOT_TABLE);

        if (tables.contains(id)) {
            ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, id);

            if (id.getPath().startsWith("entities/")) {
                Optional<EntityType<?>> type = BuiltInRegistries.ENTITY_TYPE.getOptional(id.withPath(id.getPath().substring("entities/".length())));
                return new Source(key, type.map(entityType -> createEntity(entityType, level)).orElse(null), null);
            }

            if (id.getPath().startsWith("blocks/")) {
                Optional<Block> block = BuiltInRegistries.BLOCK.getOptional(id.withPath(id.getPath().substring("blocks/".length())));
                return new Source(key, null, block.map(Block::defaultBlockState).orElse(null));
            }

            return new Source(key, null, null);
        }

        Optional<EntityType<?>> entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(id);

        if (entityType.isPresent()) {
            ResourceKey<LootTable> key = entityType.get().getDefaultLootTable();

            if (key == BuiltInLootTables.EMPTY) {
                throw ExecutionResultException.invalidArguments("%s has no default loot table".formatted(id));
            }

            return new Source(key, createEntity(entityType.get(), level), null);
        }

        Optional<Block> block = BuiltInRegistries.BLOCK.getOptional(id);

        if (block.isPresent()) {
            ResourceKey<LootTable> key = block.get().getLootTable();

            if (key == BuiltInLootTables.EMPTY) {
                throw ExecutionResultException.invalidArguments("%s has no loot table; it drops nothing".formatted(id));
            }

            return new Source(key, null, block.get().defaultBlockState());
        }

        throw ExecutionResultException.invalidArguments("Unknown loot table, entity or block %s".formatted(id));
    }

    private static Entity createEntity(EntityType<?> type, ServerLevel level) {
        return type.create(level);
    }

    @SuppressWarnings("unchecked")
    private static <T> void setParameter(LootParams.Builder builder, LootContextParam<T> param, Object value) {
        builder.withParameter(param, (T) value);
    }

    private record Source(ResourceKey<LootTable> table, Entity entity, BlockState blockState) {
    }
}
