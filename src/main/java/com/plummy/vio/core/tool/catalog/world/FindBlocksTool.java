package com.plummy.vio.core.tool.catalog.world;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.plummy.vio.core.access.AccessLevel;
import com.plummy.vio.core.tool.executor.ExecutionResult;
import com.plummy.vio.core.tool.ServerTool;
import com.plummy.vio.core.tool.value.argument.*;
import com.plummy.vio.core.tool.value.catalog.BlockPosition;
import com.plummy.vio.core.tool.value.catalog.BlockPredicate;
import com.plummy.vio.core.tool.value.catalog.Page;
import com.plummy.vio.core.tool.value.result.ResultValueJsonSerializer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.*;

public class FindBlocksTool implements ServerTool {

    private static final int MAX_LIMIT = 100;

    private static final Comparator<Match> NEAREST_FIRST = Comparator.comparingDouble(Match::distanceSquared)
            .thenComparingInt(match -> match.pos().getX())
            .thenComparingInt(match -> match.pos().getY())
            .thenComparingInt(match -> match.pos().getZ());

    @Override
    public String getName() {
        return "find_blocks";
    }

    @Override
    public String getDescription() {
        return "Search loaded chunks for blocks matching a predicate, nearest first. The predicate takes an id, a #tag and partial states, e.g. minecraft:chest, #minecraft:logs, minecraft:oak_stairs[half=top]. Unloaded chunks are skipped and counted.";
    }

    @Override
    public SequencedMap<String, ArgumentDefinition> getArgumentSchema() {
        SequencedMap<String, ArgumentDefinition> schema = new LinkedHashMap<>();

        schema.put("status", ArgumentDefinition.status());
        schema.put("block_predicate", new ArgumentDefinition(ArgumentType.BLOCK_PREDICATE, "Block predicate: id, #tag, optional [state=value]."));
        schema.put("center", new ArgumentDefinition(ArgumentType.BLOCK_POSITION, "Search center [x, y, z]."));
        schema.put("dimension", new ArgumentDefinition(ArgumentType.DIMENSION, "Dimension id."));
        schema.put("radius_blocks", new ArgumentDefinition(ArgumentType.INTEGER, "Search radius, at most 256 (server-configurable)."));
        schema.put("page", ArgumentDefinition.page(MAX_LIMIT, 20, "blocks"));

        return schema;
    }

    @Override
    public AccessLevel getAccessLevel() {
        return AccessLevel.BASIC;
    }

    @Override
    public ExecutionResult execute(Context context, Arguments arguments) {
        ServerLevel level = context.getLevel(arguments.getDimension("dimension"));

        BlockPos center = arguments.getBlockPosition("center").toBlockPos();
        BlockPredicate predicate = arguments.getBlockPredicate("block_predicate");
        int radius = arguments.getInt("radius_blocks", 1, 256);
        Page page = arguments.getPage("page", MAX_LIMIT);

        context.requireLoaded(level, center);

        int keep = page.getOffset() + page.getLimit();
        PriorityQueue<Match> nearest = new PriorityQueue<>(NEAREST_FIRST.reversed());
        int total = 0;
        int skippedChunks = 0;
        long radiusSquared = (long) radius * radius;

        int minY = Math.max(level.getMinBuildHeight(), center.getY() - radius);
        int maxY = Math.min(level.getMaxBuildHeight() - 1, center.getY() + radius);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int chunkX = SectionPos.blockToSectionCoord(center.getX() - radius); chunkX <= SectionPos.blockToSectionCoord(center.getX() + radius); chunkX++) {
            for (int chunkZ = SectionPos.blockToSectionCoord(center.getZ() - radius); chunkZ <= SectionPos.blockToSectionCoord(center.getZ() + radius); chunkZ++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);

                if (chunk == null) {
                    skippedChunks++;
                    continue;
                }

                for (int sectionY = SectionPos.blockToSectionCoord(minY); sectionY <= SectionPos.blockToSectionCoord(maxY); sectionY++) {
                    LevelChunkSection section = chunk.getSection(chunk.getSectionIndexFromSectionY(sectionY));

                    if (section.hasOnlyAir() || !section.getStates().maybeHas(state -> predicate.matchesState(level, state))) {
                        continue;
                    }

                    for (int localY = 0; localY < 16; localY++) {
                        int y = SectionPos.sectionToBlockCoord(sectionY) + localY;

                        if (y < minY || y > maxY) {
                            continue;
                        }

                        for (int localZ = 0; localZ < 16; localZ++) {
                            for (int localX = 0; localX < 16; localX++) {
                                int x = SectionPos.sectionToBlockCoord(chunkX) + localX;
                                int z = SectionPos.sectionToBlockCoord(chunkZ) + localZ;
                                long distanceSquared = square(x - center.getX()) + square(y - center.getY()) + square(z - center.getZ());

                                if (distanceSquared > radiusSquared) {
                                    continue;
                                }

                                BlockState state = section.getBlockState(localX, localY, localZ);

                                if (!predicate.matches(level, pos.set(x, y, z), state)) {
                                    continue;
                                }

                                total++;
                                nearest.add(new Match(new BlockPos(x, y, z), state, distanceSquared));

                                if (nearest.size() > keep) {
                                    nearest.poll();
                                }
                            }
                        }
                    }
                }
            }
        }

        List<Match> sorted = new ArrayList<>(nearest);
        sorted.sort(NEAREST_FIRST);

        JsonArray matches = new JsonArray();

        for (Match match : page.apply(sorted)) {
            JsonObject json = new JsonObject();

            json.add("pos", BlockPosition.of(match.pos()).accept(ResultValueJsonSerializer.getInstance()));
            json.addProperty("block", BuiltInRegistries.BLOCK.getKey(match.state().getBlock()).toString());
            json.addProperty("distance_blocks", Math.round(Math.sqrt(match.distanceSquared()) * 10) / 10.0);

            matches.add(json);
        }

        JsonObject json = new JsonObject();

        json.add("matches", matches);
        json.add("page", page.withTotal(total).accept(ResultValueJsonSerializer.getInstance()));
        json.addProperty("skipped_unloaded_chunks", skippedChunks);

        return ExecutionResult.success(json);
    }

    private static long square(int value) {
        return (long) value * value;
    }

    private record Match(BlockPos pos, BlockState state, long distanceSquared) {
    }
}
