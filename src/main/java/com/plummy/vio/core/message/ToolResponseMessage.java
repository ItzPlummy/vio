package com.plummy.vio.core.message;

import com.plummy.vio.core.section.Section;
import com.plummy.vio.core.tool.ExecutionResult;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;

public class ToolResponseMessage implements Message {

    private final SequencedMap<String, ExecutionResult> toolExecutionResults;
    private final Instant time;

    public ToolResponseMessage(SequencedMap<String, ExecutionResult> toolExecutionResults, Instant time) {
        this.toolExecutionResults = new LinkedHashMap<>(toolExecutionResults);
        this.time = time;
    }

    public static ToolResponseMessage of(SequencedMap<String, ExecutionResult> toolExecutionResults) {
        return new ToolResponseMessage(toolExecutionResults, Message.now());
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

    @Override
    public Instant getTime() {
        return time;
    }

    public SequencedMap<String, ExecutionResult> getToolExecutionResults() {
        return Collections.unmodifiableSequencedMap(toolExecutionResults);
    }
}
