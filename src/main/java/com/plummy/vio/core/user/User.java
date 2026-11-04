package com.plummy.vio.core.user;

import com.plummy.vio.core.access.AccessLevel;

public sealed interface User permits PlayerUser {

    AccessLevel getAccessLevel();

    <V> V accept(UserVisitor<V> visitor);
}
