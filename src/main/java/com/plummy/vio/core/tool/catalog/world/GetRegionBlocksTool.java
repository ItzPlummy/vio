package com.plummy.vio.core.tool.catalog.world;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.plummy.vio.core.access.AccessLevel;
import com.plummy.vio.core.tool.executor.ExecutionResult;
import com.plummy.vio.core.tool.ServerTool;
import com.plummy.vio.core.tool.value.argument.ArgumentDefinition;
import com.plummy.vio.core.tool.value.argument.ArgumentType;
import com.plummy.vio.core.tool.value.argument.Arguments;
import com.plummy.vio.core.tool.value.catalog.BlockPosition;
import com.plummy.vio.core.tool.value.result.ResultValueJsonSerializer;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.LinkedHashMap;
import java.util.SequencedMap;

public class GetRegionBlocksTool implements ServerTool {

    public static final String BLOCK_ENTITY_MARKER = "{…}";

    @Override
    public String getName() {
        return "get_region_blocks";
    }

    @Override
    public String getDescription() {
        return "Exact 3D snapshot of a small region as palette + layers. Rows run along +x, one row per z (north first), layers bottom to top; '.' is always air. Palette entries are full block states; %s after an entry means it has block entity data (read it with get_block_nbt).".formatted(BLOCK_ENTITY_MARKER);
    }

    @Override
    public SequencedMap<String, ArgumentDefinition> getArgumentSchema() {
        SequencedMap<String, ArgumentDefinition> schema = new LinkedHashMap<>();

        schema.put("status", ArgumentDefinition.status());
        schema.put("from", new ArgumentDefinition(ArgumentType.BLOCK_POSITION, "First corner [x, y, z], inclusive."));
        schema.put("to", new ArgumentDefinition(ArgumentType.BLOCK_POSITION, "Opposite corner [x, y, z], inclusive."));
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
        BoundingBox box = BoundingBox.fromCorners(
                arguments.getBlockPosition("from").toBlockPos(),
                arguments.getBlockPosition("to").toBlockPos()
        );

        long volume = (long) box.getXSpan() * box.getYSpan() * box.getZSpan();

        if (volume > 262144) {
            return ExecutionResult.invalidArguments("The region is %s blocks (%s×%s×%s), the maximum is 262144".formatted(volume, box.getXSpan(), box.getYSpan(), box.getZSpan()));
        }

        context.requireLoaded(level, box);

        Palette palette = new Palette();
        JsonArray layers = new JsonArray();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int y = box.minY(); y <= box.maxY(); y++) {
            JsonArray rows = new JsonArray();

            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                StringBuilder row = new StringBuilder();

                for (int x = box.minX(); x <= box.maxX(); x++) {
                    BlockState state = level.getBlockState(pos.set(x, y, z));

                    if (state.isAir()) {
                        row.append('.');
                        continue;
                    }

                    String entry = BlockStateParser.serialize(state) + (level.getBlockEntity(pos) != null ? BLOCK_ENTITY_MARKER : "");
                    Character key = palette.getKey(entry);

                    if (key == null) {
                        return ExecutionResult.invalidArguments("The region has more than %s distinct block states, more than the palette can hold; read a smaller region".formatted(palette.getKeys().size()));
                    }

                    row.append(key);
                }

                rows.add(row.toString());
            }

            JsonObject layer = new JsonObject();

            layer.addProperty("y_offset", y - box.minY());
            layer.add("rows", rows);

            layers.add(layer);
        }

        JsonArray size = new JsonArray();
        size.add(box.getXSpan());
        size.add(box.getYSpan());
        size.add(box.getZSpan());

        JsonObject paletteJson = new JsonObject();

        paletteJson.addProperty(".", "minecraft:air");
        palette.getKeys().forEach((entry, key) -> paletteJson.addProperty(String.valueOf(key), entry));

        JsonObject json = new JsonObject();

        json.add("origin", new BlockPosition(box.minX(), box.minY(), box.minZ()).accept(ResultValueJsonSerializer.getInstance()));
        json.add("size", size);
        json.add("palette", paletteJson);
        json.add("layers", layers);

        return ExecutionResult.success(json);
    }
}
