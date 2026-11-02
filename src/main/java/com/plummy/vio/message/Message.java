package com.plummy.vio.message;

import java.time.Instant;

public sealed interface Message permits PlayerRequestMessage, ToolRequestMessage, ToolResponseMessage, AgentResponseMessage {

    Instant getDispatchTime();

    <T> T accept(MessageVisitor<T> visitor);
}
