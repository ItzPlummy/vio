package com.plummy.vio.core.tool.world;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.plummy.vio.core.access.AccessLevel;
import com.plummy.vio.core.tool.ExecutionResult;
import com.plummy.vio.core.tool.ServerTool;
import com.plummy.vio.core.tool.argument.ArgumentDefinition;
import com.plummy.vio.core.tool.argument.ArgumentType;
import com.plummy.vio.core.tool.argument.Arguments;
import com.plummy.vio.core.tool.argument.Page;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.*;

public class CountBlocksTool implements ServerTool {

    private static final int MAX_LIMIT = 100;

    @Override
    public String getName() {
        return "count_blocks";
    }

    @Override
    public String getDescription() {
        return "Histogram of block types in a region, most common first; air is counted separately.";
    }

    @Override
    public SequencedMap<String, ArgumentDefinition> getArgumentSchema() {
        SequencedMap<String, ArgumentDefinition> schema = new LinkedHashMap<>();

        schema.put("from", new ArgumentDefinition(ArgumentType.BLOCK_POSITION, "First corner [x, y, z], inclusive."));
        schema.put("to", new ArgumentDefinition(ArgumentType.BLOCK_POSITION, "Opposite corner [x, y, z], inclusive."));
        schema.put("dimension", new ArgumentDefinition(ArgumentType.DIMENSION, "Dimension id."));
        schema.put("limit", new ArgumentDefinition(ArgumentType.INTEGER, "Block types per page, 1 to 100; 15 is a good default."));
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
        BoundingBox box = BoundingBox.fromCorners(arguments.getBlockPosition("from").toBlockPos(), arguments.getBlockPosition("to").toBlockPos());
        Page page = arguments.getPage(MAX_LIMIT);

        long volume = (long) box.getXSpan() * box.getYSpan() * box.getZSpan();

        if (volume > 262144) {
            return ExecutionResult.invalidArguments("The region is %d blocks (%d×%d×%d), the maximum is 262144.".formatted(volume, box.getXSpan(), box.getYSpan(), box.getZSpan()));
        }

        context.requireLoaded(level, box);

        Histogram histogram = scan(level, box);

        JsonArray counts = new JsonArray();

        for (Map.Entry<String, Integer> entry : page.apply(histogram.counts())) {
            JsonObject count = new JsonObject();

            count.addProperty("block", entry.getKey());
            count.addProperty("count", entry.getValue());

            counts.add(count);
        }

        JsonObject json = new JsonObject();

        json.addProperty("volume", volume);
        json.addProperty("air", histogram.air());
        json.add("counts", counts);
        page.write(json, histogram.counts().size());

        return ExecutionResult.success(json);
    }

    private static Histogram scan(ServerLevel level, BoundingBox box) {
        Map<Block, Integer> counts = new HashMap<>();
        int air = 0;

        for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
            BlockState state = level.getBlockState(pos);

            if (state.isAir()) {
                air++;
                continue;
            }

            counts.merge(state.getBlock(), 1, Integer::sum);
        }

        List<Map.Entry<String, Integer>> sorted = counts.entrySet().stream()
                .map(entry -> Map.entry(BuiltInRegistries.BLOCK.getKey(entry.getKey()).toString(), entry.getValue()))
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .toList();

        return new Histogram(sorted, air);
    }

    private record Histogram(List<Map.Entry<String, Integer>> counts, int air) {
    }
}
