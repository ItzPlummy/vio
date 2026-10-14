package com.plummy.vio.message;

import com.plummy.vio.section.Section;
import com.plummy.vio.tool.ToolExecutionResult;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;

public class ToolResponseMessage implements ConversationMessage {

    private final SequencedMap<String, ToolExecutionResult> toolExecutionResults;

    public ToolResponseMessage(SequencedMap<String, ToolExecutionResult> toolExecutionResults) {
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

    public SequencedMap<String, ToolExecutionResult> getToolExecutionResults() {
        return Collections.unmodifiableSequencedMap(toolExecutionResults);
    }
}
