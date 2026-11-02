package com.plummy.vio.message;

import com.plummy.vio.tool.executor.ExecutionResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.SequencedMap;

public final class ToolResponseMessage implements Message {

    private final SequencedMap<String, ExecutionResult> toolExecutionResults;
    private final Instant dispatchTime;

    public ToolResponseMessage(SequencedMap<String, ExecutionResult> toolExecutionResults) {
        this.toolExecutionResults = new LinkedHashMap<>(toolExecutionResults);
        this.dispatchTime = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    public ToolResponseMessage(SequencedMap<String, ExecutionResult> toolExecutionResults, Instant dispatchTime) {
        this.toolExecutionResults = new LinkedHashMap<>(toolExecutionResults);
        this.dispatchTime = dispatchTime;
    }

    @Override
    public Instant getDispatchTime() {
        return dispatchTime;
    }

    @Override
    public <T> T accept(MessageVisitor<T> visitor) {
        return visitor.visitToolResponseMessage(this);
    }

    public SequencedMap<String, ExecutionResult> getToolExecutionResults() {
        return Collections.unmodifiableSequencedMap(toolExecutionResults);
    }
}
