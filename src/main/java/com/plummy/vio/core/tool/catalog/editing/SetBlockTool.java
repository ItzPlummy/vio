package com.plummy.vio.core.tool.catalog.editing;

import com.google.gson.JsonObject;
import com.plummy.vio.core.access.AccessLevel;
import com.plummy.vio.core.tool.executor.ExecutionResult;
import com.plummy.vio.core.tool.ServerTool;
import com.plummy.vio.core.tool.value.argument.*;
import com.plummy.vio.core.tool.value.catalog.BlockPosition;
import com.plummy.vio.core.tool.value.catalog.BlockSpec;
import com.plummy.vio.core.tool.value.result.ResultValueJsonSerializer;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.LinkedHashMap;
import java.util.SequencedMap;

public class SetBlockTool implements ServerTool {

    @Override
    public String getName() {
        return "set_block";
    }

    @Override
    public String getDescription() {
        return "Place one block with an exact state and optional block entity data, like /setblock. The state is placed as given (stairs, fences and walls don't reshape to their neighbors). Use get_block_type_info for valid states. Modes: replace overwrites, keep only places into air, destroy breaks the old block first and drops its items.";
    }

    @Override
    public SequencedMap<String, ArgumentDefinition> getArgumentSchema() {
        SequencedMap<String, ArgumentDefinition> schema = new LinkedHashMap<>();

        schema.put("status", ArgumentDefinition.status());
        schema.put("position", new ArgumentDefinition(ArgumentType.BLOCK_POSITION, "Block position [x, y, z]."));
        schema.put("dimension", new ArgumentDefinition(ArgumentType.DIMENSION, "Dimension id."));
        schema.put("block", new ArgumentDefinition(ArgumentType.BLOCK_SPEC, "Block like /setblock: id with optional [state] and {nbt}, e.g. minecraft:chest[facing=north]{Items:[...]}."));
        schema.put("mode", ArgumentDefinition.enumeration(Mode.class, "replace, keep (only into air) or destroy (drops the old block)."));

        return schema;
    }

    @Override
    public AccessLevel getAccessLevel() {
        return AccessLevel.FULL;
    }

    @Override
    public ExecutionResult execute(Context context, Arguments arguments) {
        ServerLevel level = context.getLevel(arguments.getDimension("dimension"));
        BlockPos pos = arguments.getBlockPosition("position").toBlockPos();
        BlockSpec block = arguments.getBlockSpec("block");
        BlockState state = block.getState(level);
        CompoundTag nbt = block.getNbt(level);
        Mode mode = arguments.getEnum("mode", Mode.class);

        context.requireLoaded(level, pos);

        BlockState previous = level.getBlockState(pos);

        if (mode == Mode.KEEP && !previous.isAir()) {
            return ExecutionResult.failure("%s %s %s (%s) is %s, not air; mode keep places only into air".formatted(pos.getX(), pos.getY(), pos.getZ(), level.dimension().location(), BlockStateParser.serialize(previous)));
        }

        if (mode == Mode.DESTROY) {
            level.destroyBlock(pos, true);
        }

        BlockPlacer.place(level, pos, state, nbt, Block.UPDATE_ALL);

        JsonObject json = new JsonObject();

        json.add("pos", BlockPosition.of(pos).accept(ResultValueJsonSerializer.getInstance()));
        json.addProperty("previous", BlockStateParser.serialize(previous));
        json.addProperty("placed", BlockStateParser.serialize(level.getBlockState(pos)));

        return ExecutionResult.success(json);
    }

    private enum Mode {
        REPLACE,
        KEEP,
        DESTROY
    }
}
