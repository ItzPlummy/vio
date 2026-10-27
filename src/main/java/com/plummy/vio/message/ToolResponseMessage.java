package com.plummy.vio.message;

import com.plummy.vio.tool.executor.ExecutionResult;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.SequencedMap;

public class ToolResponseMessage extends AbstractMessage {

    private final SequencedMap<String, ExecutionResult> toolExecutionResults;

    public ToolResponseMessage(SequencedMap<String, ExecutionResult> toolExecutionResults) {
        this.toolExecutionResults = new LinkedHashMap<>(toolExecutionResults);
    }

    public ToolResponseMessage(SequencedMap<String, ExecutionResult> toolExecutionResults, Instant dispatchTime) {
        super(dispatchTime);

        this.toolExecutionResults = new LinkedHashMap<>(toolExecutionResults);
    }

    @Override
    public <T> T accept(MessageVisitor<T> visitor) {
        return visitor.visitToolResponseMessage(this);
    }

    public SequencedMap<String, ExecutionResult> getToolExecutionResults() {
        return Collections.unmodifiableSequencedMap(toolExecutionResults);
    }
}
