package com.plummy.vio.message;

import com.plummy.vio.section.Section;
import com.plummy.vio.section.TextSection;
import com.plummy.vio.tool.ToolExecutionRequest;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;

public class ToolRequestMessage implements ConversationMessage {

    private final TextSection activityDescription;
    private final SequencedMap<String, ToolExecutionRequest> toolExecutionRequests;

    public ToolRequestMessage(TextSection activityDescription, SequencedMap<String, ToolExecutionRequest> toolExecutionRequests) {
        this.activityDescription = activityDescription;
        this.toolExecutionRequests = new LinkedHashMap<>(toolExecutionRequests);
    }

    @Override
    public MessageType getType() {
        return MessageType.TOOL_REQUEST;
    }

    @Override
    public List<Section> getSections() {
        return List.of(
                getActivityDescription()
        );
    }

    @Override
    public <T> T accept(MessageVisitor<T> visitor) {
        return visitor.visitToolRequestMessage(this);
    }

    public TextSection getActivityDescription() {
        return activityDescription;
    }

    public SequencedMap<String, ToolExecutionRequest> getToolExecutionRequests() {
        return Collections.unmodifiableSequencedMap(toolExecutionRequests);
    }
}
