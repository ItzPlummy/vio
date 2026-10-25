package com.plummy.vio.core.tool.argument;

import com.plummy.vio.core.tool.result.ResultValue;
import com.plummy.vio.core.tool.result.ResultValueVisitor;
import net.minecraft.core.BlockPos;

public class BlockPosition implements Argument, ResultValue {

    private final int x;
    private final int y;
    private final int z;

    public BlockPosition(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static BlockPosition of(BlockPos pos) {
        return new BlockPosition(pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public <T> T accept(ResultValueVisitor<T> visitor) {
        return visitor.visitBlockPosition(this);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public BlockPos toBlockPos() {
        return new BlockPos(x, y, z);
    }
}
