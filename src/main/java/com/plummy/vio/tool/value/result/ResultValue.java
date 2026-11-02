package com.plummy.vio.tool.value.result;

public interface ResultValue {

    <T> T accept(ResultValueVisitor<T> visitor);
}
