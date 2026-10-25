package com.plummy.vio.core.tool.value.catalog;

import com.plummy.vio.core.tool.value.argument.Argument;
import com.plummy.vio.core.tool.value.result.ResultValue;
import com.plummy.vio.core.tool.value.result.ResultValueVisitor;
import net.minecraft.world.phys.Vec3;

public class Position implements Argument, ResultValue {

    private final double x;
    private final double y;
    private final double z;

    public Position(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static Position of(Vec3 pos) {
        return new Position(pos.x, pos.y, pos.z);
    }

    @Override
    public <T> T accept(ResultValueVisitor<T> visitor) {
        return visitor.visitPosition(this);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }
}
