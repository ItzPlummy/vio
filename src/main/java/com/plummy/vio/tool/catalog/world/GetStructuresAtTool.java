package com.plummy.vio.tool.catalog.world;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.plummy.vio.access.AccessLevel;
import com.plummy.vio.tool.ServerTool;
import com.plummy.vio.tool.executor.ExecutionResult;
import com.plummy.vio.tool.value.argument.ArgumentDefinition;
import com.plummy.vio.tool.value.argument.ArgumentType;
import com.plummy.vio.tool.value.argument.Arguments;
import com.plummy.vio.tool.value.catalog.BlockPosition;
import com.plummy.vio.tool.value.result.ResultValueJsonSerializer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.LinkedHashMap;
import java.util.SequencedMap;

public class GetStructuresAtTool implements ServerTool {

    @Override
    public String getName() {
        return "get_structures_at";
    }

    @Override
    public String getDescription() {
        return "Which structures a position is inside ('where am I'), complementing /locate. inside_piece = true means inside one of the structure's actual pieces, false means inside its overall bounding box but between pieces. Structures that start in unloaded chunks can't be checked and are only counted.";
    }

    @Override
    public SequencedMap<String, ArgumentDefinition> getArgumentSchema() {
        SequencedMap<String, ArgumentDefinition> schema = new LinkedHashMap<>();

        schema.put("status", ArgumentDefinition.status());
        schema.put("position", new ArgumentDefinition(ArgumentType.BLOCK_POSITION, "Position [x, y, z]."));
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

        StructureLookup.Result result = StructureLookup.getStructuresAt(level, pos);
        JsonArray structures = new JsonArray();

        for (StructureLookup.Found found : result.structures()) {
            BoundingBox box = found.start().getBoundingBox();
            JsonObject boundingBox = new JsonObject();

            boundingBox.add("min", new BlockPosition(box.minX(), box.minY(), box.minZ()).accept(ResultValueJsonSerializer.getInstance()));
            boundingBox.add("max", new BlockPosition(box.maxX(), box.maxY(), box.maxZ()).accept(ResultValueJsonSerializer.getInstance()));

            JsonObject structure = new JsonObject();

            structure.addProperty("id", found.id());
            structure.addProperty("inside_piece", found.insidePiece());
            structure.add("bounding_box", boundingBox);

            structures.add(structure);
        }

        JsonObject json = new JsonObject();

        json.add("structures", structures);

        if (result.unloadedStarts() > 0) {
            json.addProperty("unchecked_unloaded_starts", result.unloadedStarts());
        }

        return ExecutionResult.success(json);
    }
}
