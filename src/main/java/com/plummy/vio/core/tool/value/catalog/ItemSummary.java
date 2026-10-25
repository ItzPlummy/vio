package com.plummy.vio.core.tool.value.catalog;

import com.plummy.vio.core.tool.value.result.ResultValue;
import com.plummy.vio.core.tool.value.result.ResultValueVisitor;
import net.minecraft.world.item.ItemStack;

public class ItemSummary implements ResultValue {

    private final ItemStack itemStack;

    public ItemSummary(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    @Override
    public <T> T accept(ResultValueVisitor<T> visitor) {
        return visitor.visitItemSummary(this);
    }

    public ItemStack getItemStack() {
        return itemStack;
    }
}
