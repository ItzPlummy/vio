package com.plummy.vio.tool.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

// A block written like /setblock: minecraft:chest[facing=east]{Items:[...]}.
// Parsing needs the block registry, so the text is parsed on first use.
public class BlockSpec implements Argument {

    private final String text;
    private BlockStateParser.BlockResult block;

    public BlockSpec(String text) {
        this.text = text;
    }

    public BlockState getState(ServerLevel level) {
        return getBlock(level).blockState();
    }

    // Block entity data to place with the state, or null.
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
                throw new Argument.ParseException("\"%s\" is not a valid block: %s".formatted(text, e.getMessage()));
            }

            if (parsed.nbt() != null && !parsed.blockState().hasBlockEntity()) {
                throw new Argument.ParseException("\"%s\" has NBT, but the block has no block entity to take it.".formatted(text));
            }

            block = parsed;
        }

        return block;
    }
}
