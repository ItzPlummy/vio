package com.plummy.vio.tool.result;

// A value tools put in their results; shared by tools so every position or item reads the same to the model.
public interface ResultValue {

    <T> T accept(ResultValueVisitor<T> visitor);
}
