package com.plummy.vio.core.message;

import com.plummy.vio.core.section.Section;
import com.plummy.vio.core.tool.executor.ExecutionResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;

public class ToolResponseMessage extends AbstractMessage {

    private final SequencedMap<String, ExecutionResult> toolExecutionResults;

    public ToolResponseMessage(SequencedMap<String, ExecutionResult> toolExecutionResults, Instant time) {
        super(time);
        this.toolExecutionResults = new LinkedHashMap<>(toolExecutionResults);
    }

    public static ToolResponseMessage of(SequencedMap<String, ExecutionResult> toolExecutionResults) {
        return new ToolResponseMessage(
                toolExecutionResults,
                Instant.now().truncatedTo(ChronoUnit.MILLIS)
        );
    }

    @Override
    public MessageType getType() {
        return MessageType.TOOL_RESPONSE;
    }

    @Override
    public List<Section> getSections() {
        return List.of();
    }

    @Override
    public <T> T accept(MessageVisitor<T> visitor) {
        return visitor.visitToolResponseMessage(this);
    }

    public SequencedMap<String, ExecutionResult> getToolExecutionResults() {
        return Collections.unmodifiableSequencedMap(toolExecutionResults);
    }
}
