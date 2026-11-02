package com.plummy.vio.tool.value.result;

import com.plummy.vio.tool.value.catalog.*;

public interface ResultValueVisitor<T> {

    T visitPosition(Position value);

    T visitBlockPosition(BlockPosition value);

    T visitItemSummary(ItemSummary value);

    T visitItemList(ItemList value);

    T visitPage(Page value);
}
