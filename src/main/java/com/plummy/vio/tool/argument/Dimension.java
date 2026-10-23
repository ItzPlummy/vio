package com.plummy.vio.tool.argument;

import net.minecraft.resources.ResourceLocation;

// A dimension id; resolved to a loaded level by ServerTool.Context.getLevel.
public class Dimension implements Argument {

    private final ResourceLocation id;

    public Dimension(ResourceLocation id) {
        this.id = id;
    }

    public ResourceLocation getId() {
        return id;
    }
}
