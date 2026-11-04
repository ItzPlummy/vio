package com.plummy.vio.core.turn;

import java.time.Instant;

public record ToolExecution(String executionDescription, Instant executionStartTime) {
}
