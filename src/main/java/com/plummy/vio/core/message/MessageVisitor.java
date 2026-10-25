package com.plummy.vio.core.message;

public interface MessageVisitor<T> {

    T visitPlayerRequestMessage(PlayerRequestMessage message);

    T visitToolRequestMessage(ToolRequestMessage message);

    T visitToolResponseMessage(ToolResponseMessage message);

    T visitAgentResponseMessage(AgentResponseMessage message);
}
