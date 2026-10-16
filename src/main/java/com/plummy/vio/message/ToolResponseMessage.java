package com.plummy.vio.message;

import com.plummy.vio.section.Section;
import com.plummy.vio.tool.Tool;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;

public class ToolResponseMessage implements Message {

    private final SequencedMap<String, Tool.ExecutionResult> toolExecutionResults;
    private final Instant time;

    public ToolResponseMessage(SequencedMap<String, Tool.ExecutionResult> toolExecutionResults, Instant time) {
        this.toolExecutionResults = new LinkedHashMap<>(toolExecutionResults);
        this.time = time;
    }

    public static ToolResponseMessage of(SequencedMap<String, Tool.ExecutionResult> toolExecutionResults) {
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

    public SequencedMap<String, Tool.ExecutionResult> getToolExecutionResults() {
        return Collections.unmodifiableSequencedMap(toolExecutionResults);
    }
}
