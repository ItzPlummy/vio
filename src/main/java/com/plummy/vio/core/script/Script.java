package com.plummy.vio.core.script;

import com.plummy.vio.core.script.dsl.CompiledScript;
import com.plummy.vio.core.storage.Storage;

import java.time.Instant;
import java.util.UUID;

public interface Script extends Storage.Item<String> {

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
