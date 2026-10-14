package com.plummy.vio.message;

import com.plummy.vio.section.Section;
import com.plummy.vio.tool.Tool;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;

public class ToolResponseMessage implements ConversationMessage {

    private final SequencedMap<String, Tool.ExecutionResult> toolExecutionResults;

    public ToolResponseMessage(SequencedMap<String, Tool.ExecutionResult> toolExecutionResults) {
        this.toolExecutionResults = new LinkedHashMap<>(toolExecutionResults);
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

    public SequencedMap<String, Tool.ExecutionResult> getToolExecutionResults() {
        return Collections.unmodifiableSequencedMap(toolExecutionResults);
    }
}
