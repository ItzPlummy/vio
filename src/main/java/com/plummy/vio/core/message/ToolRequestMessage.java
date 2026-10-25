package com.plummy.vio.core.message;

import com.plummy.vio.core.section.ActivityDescriptionSection;
import com.plummy.vio.core.section.Section;
import com.plummy.vio.core.tool.executor.ExecutionRequest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;

public class ToolRequestMessage extends AbstractMessage {

    private final ActivityDescriptionSection activityDescription;
    private final SequencedMap<String, ExecutionRequest> toolExecutionRequests;

    public ToolRequestMessage(ActivityDescriptionSection activityDescription, SequencedMap<String, ExecutionRequest> toolExecutionRequests, Instant time) {
        super(time);
        this.activityDescription = activityDescription;
        this.toolExecutionRequests = new LinkedHashMap<>(toolExecutionRequests);
    }

    public static ToolRequestMessage of(SequencedMap<String, ExecutionRequest> toolExecutionRequests) {
        String activityDescription = toolExecutionRequests.values().stream()
                .map(request -> request.getArguments().getStatus())
                .filter(status -> !status.isBlank())
                .findFirst()
                .orElse("");

        return new ToolRequestMessage(
                new ActivityDescriptionSection(activityDescription),
                toolExecutionRequests,
                Instant.now().truncatedTo(ChronoUnit.MILLIS)
        );
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

    public ActivityDescriptionSection getActivityDescription() {
        return activityDescription;
    }

    public SequencedMap<String, ExecutionRequest> getToolExecutionRequests() {
        return Collections.unmodifiableSequencedMap(toolExecutionRequests);
    }
}
