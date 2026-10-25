package com.plummy.vio.core.tool.value.catalog;

import com.plummy.vio.core.tool.value.argument.Argument;
import net.minecraft.resources.ResourceLocation;

public class Dimension implements Argument {

    private final ResourceLocation id;

    public Dimension(ResourceLocation id) {
        this.id = id;
    }

    public ResourceLocation getId() {
        return id;
    }
}
