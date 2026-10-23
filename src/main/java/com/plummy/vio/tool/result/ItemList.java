package com.plummy.vio.tool.result;

import net.minecraft.world.item.ItemStack;

import java.util.List;

// Consecutive slots starting at firstSlot; empty slots are left out and the others keep their slot number.
public class ItemList implements ResultValue {

    private final List<ItemStack> stacks;
    private final int firstSlot;

    public ItemList(List<ItemStack> stacks, int firstSlot) {
        this.stacks = List.copyOf(stacks);
        this.firstSlot = firstSlot;
    }

    @Override
    public <T> T accept(ResultValueVisitor<T> visitor) {
        return visitor.visitItemList(this);
    }

    public List<ItemStack> getStacks() {
        return stacks;
    }

    public int getFirstSlot() {
        return firstSlot;
    }
}
