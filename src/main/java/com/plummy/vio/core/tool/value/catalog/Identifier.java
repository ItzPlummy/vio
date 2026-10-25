package com.plummy.vio.core.tool.value.catalog;

import com.plummy.vio.core.tool.value.argument.Argument;
import net.minecraft.resources.ResourceLocation;

public class Identifier implements Argument {

    private final ResourceLocation id;
    private final boolean isTag;

    public Identifier(ResourceLocation id, boolean isTag) {
        this.id = id;
        this.isTag = isTag;
    }

    public ResourceLocation getId() {
        return id;
    }

    public boolean isTag() {
        return isTag;
    }

    @Override
    public String toString() {
        return isTag() ? "#%s".formatted(getId()) : getId().toString();
    }
}
