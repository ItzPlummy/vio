package com.plummy.vio.tool.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Either;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Map;

// A block predicate like /execute if block: minecraft:chest, #minecraft:logs, minecraft:oak_stairs[half=top].
// Parsing needs the block registry, so the text is parsed on the first test; an invalid predicate only fails once something is tested.
public class BlockPredicate implements Argument {

    private final String text;
    private Either<BlockStateParser.BlockResult, BlockStateParser.TagResult> predicate;

    public BlockPredicate(String text) {
        this.text = text;
    }

    // Ignores NBT, so whole chunk sections can be skipped by their palette.
    public boolean test(ServerLevel level, BlockState state) {
        return getPredicate(level).map(
                block -> {
                    if (!state.is(block.blockState().getBlock())) {
                        return false;
                    }

                    for (Property<?> property : block.properties().keySet()) {
                        if (state.getValue(property) != block.blockState().getValue(property)) {
                            return false;
                        }
                    }

                    return true;
                },
                tag -> {
                    if (!state.is(tag.tag())) {
                        return false;
                    }

                    for (Map.Entry<String, String> entry : tag.vagueProperties().entrySet()) {
                        Property<?> property = state.getBlock().getStateDefinition().getProperty(entry.getKey());

                        if (property == null || property.getValue(entry.getValue()).map(value -> value != state.getValue(property)).orElse(true)) {
                            return false;
                        }
                    }

                    return true;
                }
        );
    }

    public boolean test(ServerLevel level, BlockPos pos, BlockState state) {
        if (!test(level, state)) {
            return false;
        }

        CompoundTag nbt = getPredicate(level).map(BlockStateParser.BlockResult::nbt, BlockStateParser.TagResult::nbt);

        if (nbt == null) {
            return true;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);

        return blockEntity != null && NbtUtils.compareNbt(nbt, blockEntity.saveWithFullMetadata(level.registryAccess()), true);
    }

    public String getText() {
        return text;
    }

    private Either<BlockStateParser.BlockResult, BlockStateParser.TagResult> getPredicate(ServerLevel level) {
        if (predicate == null) {
            try {
                predicate = BlockStateParser.parseForTesting(level.registryAccess().lookupOrThrow(Registries.BLOCK), text, true);
            } catch (CommandSyntaxException e) {
                throw new Argument.ParseException("\"%s\" is not a valid block predicate: %s".formatted(text, e.getMessage()));
            }
        }

        return predicate;
    }
}
