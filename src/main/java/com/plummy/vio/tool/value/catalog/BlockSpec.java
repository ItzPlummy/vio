package com.plummy.vio.tool.value.catalog;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.plummy.vio.tool.value.argument.Argument;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

public class BlockSpec implements Argument {

    private final String text;
    private BlockStateParser.BlockResult block;

    public BlockSpec(String text) {
        this.text = text;
    }

    public BlockState getState(ServerLevel level) {
        return getBlock(level).blockState();
    }

    public CompoundTag getNbt(ServerLevel level) {
        return getBlock(level).nbt();
    }

    public String getText() {
        return text;
    }

    private BlockStateParser.BlockResult getBlock(ServerLevel level) {
        if (block == null) {
            BlockStateParser.BlockResult parsed;

            try {
                parsed = BlockStateParser.parseForBlock(level.registryAccess().lookupOrThrow(Registries.BLOCK), text, true);
            } catch (CommandSyntaxException e) {
                throw new Argument.ParseException("'%s' isn't a valid block: %s".formatted(text, e.getMessage()));
            }

            if (parsed.nbt() != null && !parsed.blockState().hasBlockEntity()) {
                throw new Argument.ParseException("'%s' has NBT, but the block has no block entity to take it".formatted(text));
            }

            block = parsed;
        }

        return block;
    }
}
