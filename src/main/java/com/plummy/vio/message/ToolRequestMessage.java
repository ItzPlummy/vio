package com.plummy.vio.message;

import com.plummy.vio.tool.executor.ExecutionRequest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.SequencedMap;

public final class ToolRequestMessage implements Message {

    private final String activityDescription;
    private final SequencedMap<String, ExecutionRequest> toolExecutionRequests;
    private final Instant dispatchTime;

    public ToolRequestMessage(SequencedMap<String, ExecutionRequest> toolExecutionRequests) {
        this.activityDescription = getDescribedActivity(toolExecutionRequests);
        this.toolExecutionRequests = new LinkedHashMap<>(toolExecutionRequests);
        this.dispatchTime = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    public ToolRequestMessage(String activityDescription, SequencedMap<String, ExecutionRequest> toolExecutionRequests, Instant dispatchTime) {
        this.activityDescription = activityDescription;
        this.toolExecutionRequests = new LinkedHashMap<>(toolExecutionRequests);
        this.dispatchTime = dispatchTime;
    }

    @Override
    public Instant getDispatchTime() {
        return dispatchTime;
    }

    @Override
    public <T> T accept(MessageVisitor<T> visitor) {
        return visitor.visitToolRequestMessage(this);
    }

    public String getActivityDescription() {
        return activityDescription;
    }

    public SequencedMap<String, ExecutionRequest> getToolExecutionRequests() {
        return Collections.unmodifiableSequencedMap(toolExecutionRequests);
    }

    private static String getDescribedActivity(SequencedMap<String, ExecutionRequest> toolExecutionRequests) {
        return toolExecutionRequests.values().stream().map(request -> request.getArguments().getStatus()).filter(status -> !status.isBlank()).findFirst().orElse("");
    }
}
