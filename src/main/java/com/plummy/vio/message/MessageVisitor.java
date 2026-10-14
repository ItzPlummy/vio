package com.plummy.vio.message;

public interface MessageVisitor<T> {

    T visitSystemMessage(SystemMessage message);

    T visitPlayerRequestMessage(PlayerRequestMessage message);

    T visitToolRequestMessage(ToolRequestMessage message);

    T visitToolResponseMessage(ToolResponseMessage message);

    T visitAgentResponseMessage(AgentResponseMessage message);
}
