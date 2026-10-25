package com.plummy.vio.core.tool.value.catalog;

import com.plummy.vio.core.tool.value.result.ResultValue;
import com.plummy.vio.core.tool.value.result.ResultValueVisitor;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class ItemList implements ResultValue {

    private final SortedMap<Integer, ItemStack> stacks;

    public ItemList(Map<Integer, ItemStack> stacks) {
        this.stacks = Collections.unmodifiableSortedMap(new TreeMap<>(stacks));
    }

    @Override
    public <T> T accept(ResultValueVisitor<T> visitor) {
        return visitor.visitItemList(this);
    }

    public SortedMap<Integer, ItemStack> getItemStacks() {
        return stacks;
    }
}
