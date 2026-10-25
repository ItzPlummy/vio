package com.plummy.vio.core.tool.argument;

import net.minecraft.resources.ResourceLocation;

// A registry id like minecraft:stone, or a tag like #minecraft:logs.
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
        return isTag() ? "#" + getId() : getId().toString();
    }
}
