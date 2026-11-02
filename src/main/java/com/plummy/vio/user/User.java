package com.plummy.vio.user;

import com.plummy.vio.access.AccessLevel;
import com.plummy.vio.storage.StorageItem;

public sealed interface User<K> extends StorageItem<K> permits PlayerUser {

    AccessLevel getAccessLevel();

    <T> T accept(UserVisitor<T> visitor);
}
