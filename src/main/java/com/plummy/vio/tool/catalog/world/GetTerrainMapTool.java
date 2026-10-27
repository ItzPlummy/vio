package com.plummy.vio.tool.catalog.world;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.plummy.vio.access.AccessLevel;
import com.plummy.vio.tool.ServerTool;
import com.plummy.vio.tool.executor.ExecutionResult;
import com.plummy.vio.tool.value.argument.ArgumentDefinition;
import com.plummy.vio.tool.value.argument.ArgumentType;
import com.plummy.vio.tool.value.argument.Arguments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.LinkedHashMap;
import java.util.SequencedMap;

public class GetTerrainMapTool implements ServerTool {

    @Override
    public String getName() {
        return "get_terrain_map";
    }

    @Override
    public String getDescription() {
        return "Top-down map around a point: the surface block and surface height of every column. Rows run along +x, one row per z, north (lowest z) first; heights are offsets from height_base (the surface block is at height_base + offset). Unloaded columns are '?' and null. world_surface includes water and leaves, motion_blocking_no_leaves skips leaves, ocean_floor shows the ground under water. Not available in dimensions with a ceiling like the Nether; use get_region_blocks there.";
    }

    @Override
    public SequencedMap<String, ArgumentDefinition> getArgumentSchema() {
        SequencedMap<String, ArgumentDefinition> schema = new LinkedHashMap<>();

        schema.put("status", ArgumentDefinition.status());
        schema.put("x", new ArgumentDefinition(ArgumentType.INTEGER, "Center X."));
        schema.put("z", new ArgumentDefinition(ArgumentType.INTEGER, "Center Z."));
        schema.put("dimension", new ArgumentDefinition(ArgumentType.DIMENSION, "Dimension id."));
        schema.put("radius_blocks", new ArgumentDefinition(ArgumentType.INTEGER, "Map radius, at most 32 (server-configurable)."));
        schema.put("heightmap", ArgumentDefinition.enumeration(HeightmapType.class, "Which surface to map."));

        return schema;
    }

    @Override
    public AccessLevel getAccessLevel() {
        return AccessLevel.BASIC;
    }

    @Override
    public ExecutionResult execute(Context context, Arguments arguments) {
        ServerLevel level = context.getLevel(arguments.getDimension("dimension"));

        int centerX = arguments.getInt("x");
        int centerZ = arguments.getInt("z");
        int radius = arguments.getInt("radius_blocks", 1, 32);
        Heightmap.Types heightmap = arguments.getEnum("heightmap", HeightmapType.class).getType();

        if (level.dimensionType().hasCeiling()) {
            return ExecutionResult.invalidArguments("%s has a ceiling, so heightmaps hit the roof; use get_region_blocks instead".formatted(level.dimension().location().toString()));
        }

        int size = radius * 2 + 1;
        Integer[][] heights = new Integer[size][size];
        String[][] blocks = new String[size][size];
        int minHeight = Integer.MAX_VALUE;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) {
                int x = centerX - radius + column;
                int z = centerZ - radius + row;
                LevelChunk chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);

                if (chunk == null) {
                    continue;
                }

                int surfaceY = chunk.getHeight(heightmap, x & 15, z & 15);

                heights[row][column] = surfaceY;
                blocks[row][column] = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos.set(x, surfaceY, z)).getBlock()).toString();
                minHeight = Math.min(minHeight, surfaceY);
            }
        }

        Palette palette = new Palette();
        JsonArray surface = new JsonArray();
        JsonArray heightRows = new JsonArray();

        for (int row = 0; row < size; row++) {
            StringBuilder line = new StringBuilder();
            JsonArray heightRow = new JsonArray();

            for (int column = 0; column < size; column++) {
                String block = blocks[row][column];

                if (block == null) {
                    line.append('?');
                    heightRow.add((Number) null);
                    continue;
                }

                Character key = palette.getKey(block);

                if (key == null) {
                    return ExecutionResult.invalidArguments("Too many distinct surface blocks for the legend; use a smaller radius");
                }

                line.append(key);
                heightRow.add(heights[row][column] - minHeight);
            }

            surface.add(line.toString());
            heightRows.add(heightRow);
        }

        JsonArray center = new JsonArray();
        center.add(centerX);
        center.add(centerZ);

        JsonObject legend = new JsonObject();
        palette.getKeys().forEach((block, key) -> legend.addProperty(String.valueOf(key), block));

        JsonObject json = new JsonObject();

        json.add("center", center);
        json.add("legend", legend);
        json.add("surface", surface);

        if (minHeight == Integer.MAX_VALUE) {
            json.add("height_base", null);
        } else {
            json.addProperty("height_base", minHeight);
        }

        json.add("heights", heightRows);

        return ExecutionResult.success(json);
    }

    private enum HeightmapType {
        WORLD_SURFACE(Heightmap.Types.WORLD_SURFACE),
        MOTION_BLOCKING_NO_LEAVES(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES),
        OCEAN_FLOOR(Heightmap.Types.OCEAN_FLOOR);

        private final Heightmap.Types type;

        HeightmapType(Heightmap.Types type) {
            this.type = type;
        }

        public Heightmap.Types getType() {
            return type;
        }
    }
}
