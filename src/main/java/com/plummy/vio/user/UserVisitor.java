package com.plummy.vio.user;

public interface UserVisitor<T> {

    T visitPlayerUser(PlayerUser playerUser);
}
