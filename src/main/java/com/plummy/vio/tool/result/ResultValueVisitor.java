package com.plummy.vio.tool.result;

import com.plummy.vio.tool.argument.BlockPosition;
import com.plummy.vio.tool.argument.Position;

public interface ResultValueVisitor<T> {

    T visitPosition(Position value);

    T visitBlockPosition(BlockPosition value);

    T visitItemSummary(ItemSummary value);

    T visitItemList(ItemList value);
}
