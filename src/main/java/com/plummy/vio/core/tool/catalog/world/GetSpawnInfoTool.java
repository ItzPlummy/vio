package com.plummy.vio.core.tool.catalog.world;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.plummy.vio.core.access.AccessLevel;
import com.plummy.vio.core.tool.executor.ExecutionResult;
import com.plummy.vio.core.tool.ServerTool;
import com.plummy.vio.core.tool.value.argument.ArgumentDefinition;
import com.plummy.vio.core.tool.value.argument.ArgumentType;
import com.plummy.vio.core.tool.value.argument.Arguments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.StructureSpawnOverride;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;

public class GetSpawnInfoTool implements ServerTool {

    private static final List<MobCategory> CATEGORIES = List.of(
            MobCategory.MONSTER, MobCategory.CREATURE, MobCategory.AMBIENT, MobCategory.AXOLOTLS,
            MobCategory.UNDERGROUND_WATER_CREATURE, MobCategory.WATER_CREATURE, MobCategory.WATER_AMBIENT
    );

    // Monsters roll a light level of 0-7 against the raw light in the overworld; brighter spots never pass.
    private static final int MAX_MONSTER_RAW_LIGHT = 7;

    @Override
    public String getName() {
        return "get_spawn_info";
    }

    @Override
    public String getDescription() {
        return "What can naturally spawn at a position and why: biome, light, the spawn lists per category (weights and group sizes), and the structure whose spawn list replaces the biome's (swamp hut, ocean monument, pillager outpost, ...). hostile_can_spawn is an approximation of the natural spawner rules (light, floor, free space), not a guarantee.";
    }

    @Override
    public SequencedMap<String, ArgumentDefinition> getArgumentSchema() {
        SequencedMap<String, ArgumentDefinition> schema = new LinkedHashMap<>();

        schema.put("status", ArgumentDefinition.status());
        schema.put("position", new ArgumentDefinition(ArgumentType.BLOCK_POSITION, "Spawning space [x, y, z], y at the feet."));
        schema.put("dimension", new ArgumentDefinition(ArgumentType.DIMENSION, "Dimension id."));

        return schema;
    }

    @Override
    public AccessLevel getAccessLevel() {
        return AccessLevel.BASIC;
    }

    @Override
    public ExecutionResult execute(Context context, Arguments arguments) {
        ServerLevel level = context.getLevel(arguments.getDimension("dimension"));
        BlockPos pos = arguments.getBlockPosition("position").toBlockPos();

        context.requireLoaded(level, pos);

        Holder<Biome> biome = level.getBiome(pos);
        int blockLight = level.getBrightness(LightLayer.BLOCK, pos);
        int skyLight = level.getBrightness(LightLayer.SKY, pos);
        StructureLookup.Result structures = StructureLookup.getStructuresAt(level, pos);

        JsonObject spawns = new JsonObject();
        String structureOverride = null;
        boolean monstersListed = false;

        for (MobCategory category : CATEGORIES) {
            WeightedRandomList<MobSpawnSettings.SpawnerData> list = biome.value().getMobSettings().getMobs(category);

            // A structure's own list replaces the biome's for the categories it overrides, within its pieces or whole box.
            for (StructureLookup.Found found : structures.structures()) {
                StructureSpawnOverride override = found.structure().spawnOverrides().get(category);

                if (override != null && (override.boundingBox() == StructureSpawnOverride.BoundingBoxType.STRUCTURE || found.insidePiece())) {
                    list = override.spawns();
                    structureOverride = found.id();
                    break;
                }
            }

            if (category == MobCategory.MONSTER) {
                monstersListed = !list.isEmpty();
            }

            JsonArray entries = new JsonArray();

            for (MobSpawnSettings.SpawnerData data : list.unwrap()) {
                JsonObject entry = new JsonObject();

                entry.addProperty("type", BuiltInRegistries.ENTITY_TYPE.getKey(data.type).toString());
                entry.addProperty("weight", data.getWeight().asInt());
                entry.addProperty("group", "%s-%s".formatted(data.minCount, data.maxCount));

                entries.add(entry);
            }

            spawns.add(category.getName(), entries);
        }

        List<String> reasons = new ArrayList<>();
        boolean hostileCanSpawn = checkHostile(level, pos, blockLight, reasons);

        if (!monstersListed) {
            hostileCanSpawn = false;
            reasons.add("no monsters in the spawn list here");
        }

        JsonObject json = new JsonObject();

        json.addProperty("biome", biome.unwrapKey().map(key -> key.location().toString()).orElse("unknown"));
        json.addProperty("block_light", blockLight);
        json.addProperty("sky_light", skyLight);
        json.addProperty("hostile_can_spawn", hostileCanSpawn);
        json.addProperty("reason", String.join(", ", reasons));
        json.add("spawns", spawns);
        json.addProperty("structure_override", structureOverride);

        if (structures.unloadedStarts() > 0) {
            json.addProperty("note", "%s structure(s) referenced here start in unloaded chunks and weren't checked.".formatted(structures.unloadedStarts()));
        }

        json.addProperty("slime_chunk", WorldgenRandom.seedSlimeChunk(pos.getX() >> 4, pos.getZ() >> 4, level.getSeed(), 987234911L).nextInt(10) == 0);

        return ExecutionResult.success(json);
    }

    // Approximates NaturalSpawner and Monster.isDarkEnoughToSpawn, using a zombie as the typical hostile.
    private static boolean checkHostile(ServerLevel level, BlockPos pos, int blockLight, List<String> reasons) {
        boolean possible = true;
        int blockLightLimit = level.dimensionType().monsterSpawnBlockLightLimit();
        int rawLight = level.getMaxLocalRawBrightness(pos);

        if (blockLight > blockLightLimit) {
            possible = false;
            reasons.add("block light %s is above %s".formatted(blockLight, blockLightLimit));
        } else {
            reasons.add("block light %s".formatted(blockLight));
        }

        if (rawLight > MAX_MONSTER_RAW_LIGHT) {
            possible = false;
            reasons.add("light level %s right now is too bright".formatted(rawLight));
        }

        BlockPos floorPos = pos.below();
        BlockState floor = level.getBlockState(floorPos);

        if (!floor.isValidSpawn(level, floorPos, EntityType.ZOMBIE)) {
            possible = false;
            reasons.add("the block below can't be spawned on");
        } else {
            reasons.add("solid top surface");
        }

        BlockState feet = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());

        if (!NaturalSpawner.isValidEmptySpawnBlock(level, pos, feet, feet.getFluidState(), EntityType.ZOMBIE)
                || !NaturalSpawner.isValidEmptySpawnBlock(level, pos.above(), head, head.getFluidState(), EntityType.ZOMBIE)) {
            possible = false;
            reasons.add("no free two-block space");
        }

        return possible;
    }
}
