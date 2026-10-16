package com.plummy.vio.message;

import com.plummy.vio.section.ActivityDescriptionSection;
import com.plummy.vio.section.Section;
import com.plummy.vio.tool.Tool;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;

public class ToolRequestMessage implements ConversationMessage {

    private final ActivityDescriptionSection activityDescription;
    private final SequencedMap<String, Tool.ExecutionRequest> toolExecutionRequests;
    private final Instant time;

    public ToolRequestMessage(ActivityDescriptionSection activityDescription, SequencedMap<String, Tool.ExecutionRequest> toolExecutionRequests, Instant time) {
        this.activityDescription = activityDescription;
        this.toolExecutionRequests = new LinkedHashMap<>(toolExecutionRequests);
        this.time = time;
    }

    public static ToolRequestMessage of(SequencedMap<String, Tool.ExecutionRequest> toolExecutionRequests) {
        // With parallel calls the first status describes the round; a blank one falls back to a placeholder when shown.
        String activityDescription = toolExecutionRequests.values().stream()
                .map(request -> request.getArguments().getStatus())
                .filter(status -> !status.isBlank())
                .findFirst()
                .orElse("");

        return new ToolRequestMessage(new ActivityDescriptionSection(activityDescription), toolExecutionRequests, ConversationMessage.now());
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

    @Override
    public Instant getTime() {
        return time;
    }

    public ActivityDescriptionSection getActivityDescription() {
        return activityDescription;
    }

    public SequencedMap<String, Tool.ExecutionRequest> getToolExecutionRequests() {
        return Collections.unmodifiableSequencedMap(toolExecutionRequests);
    }
}
