package com.plummy.vio.core.tool.catalog.editing;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Clearable;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

final class BlockPlacer {

    private BlockPlacer() {
    }

    // Places one exact state like /setblock: containers are emptied first so replacing them doesn't drop their items. Callers check loading with Context.requireLoaded.
    static void place(ServerLevel level, BlockPos pos, BlockState state, CompoundTag nbt, int flags) {
        Clearable.tryClear(level.getBlockEntity(pos));
        level.setBlock(pos, state, flags);

        if (nbt != null) {
            BlockEntity blockEntity = level.getBlockEntity(pos);

            if (blockEntity != null) {
                blockEntity.loadWithComponents(nbt.copy(), level.registryAccess());
                blockEntity.setChanged();
            }
        }
    }
}
