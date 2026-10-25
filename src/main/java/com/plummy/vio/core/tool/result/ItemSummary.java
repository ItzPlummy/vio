package com.plummy.vio.core.tool.result;

import net.minecraft.world.item.ItemStack;

// One stack as count, damage, enchantments, custom name and the ids of its other components; an empty stack is null.
public class ItemSummary implements ResultValue {

    private final ItemStack stack;

    public ItemSummary(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public <T> T accept(ResultValueVisitor<T> visitor) {
        return visitor.visitItemSummary(this);
    }

    public ItemStack getStack() {
        return stack;
    }
}
