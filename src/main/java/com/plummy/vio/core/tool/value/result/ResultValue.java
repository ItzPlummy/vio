package com.plummy.vio.core.tool.value.result;

public interface ResultValue {

    <T> T accept(ResultValueVisitor<T> visitor);
}
