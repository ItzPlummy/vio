package com.plummy.vio.script;

import com.plummy.vio.script.dsl.CompiledScript;
import com.plummy.vio.storage.StorageItem;

import java.time.Instant;
import java.util.UUID;

public interface Script extends StorageItem<String> {

    ScriptType getType();

    String getName();

    String getDescription();

    ScriptState getState();

    UUID getCreator();

    Instant getCreatedAt();

    String getSource();

    void compile();

    boolean isCompiled();

    CompiledScript newInstance();

    ScriptLog getLog();

    String getError();

    void setError(String error);

    int recordFailure();

    void resetFailures();

    void dispose();

    <T> T accept(ScriptVisitor<T> visitor);
}
