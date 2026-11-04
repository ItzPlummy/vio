package com.plummy.vio.core.user;

public interface UserVisitor<T> {

    T visit(PlayerUser user);
}
