package com.plummy.vio.core.tool.value.result;

import com.plummy.vio.core.tool.value.catalog.BlockPosition;
import com.plummy.vio.core.tool.value.catalog.ItemList;
import com.plummy.vio.core.tool.value.catalog.ItemSummary;
import com.plummy.vio.core.tool.value.catalog.Page;
import com.plummy.vio.core.tool.value.catalog.Position;

public interface ResultValueVisitor<T> {

    T visitPosition(Position value);

    T visitBlockPosition(BlockPosition value);

    T visitItemSummary(ItemSummary value);

    T visitItemList(ItemList value);

    T visitPage(Page value);
}
