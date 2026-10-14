package com.plummy.vio.message;

import com.plummy.vio.section.Section;
import com.plummy.vio.section.TextSection;

import java.util.List;

public class AgentResponseMessage implements ConversationMessage {

    private final TextSection response;

    public AgentResponseMessage(TextSection response) {
        this.response = response;
    }

    public static AgentResponseMessage of(String response) {
        return new AgentResponseMessage(new TextSection(response));
    }

    @Override
    public MessageType getType() {
        return MessageType.AGENT_RESPONSE;
    }

    @Override
    public List<Section> getSections() {
        return List.of(
                getResponse()
        );
    }

    @Override
    public <T> T accept(MessageVisitor<T> visitor) {
        return visitor.visitAgentResponseMessage(this);
    }

    public TextSection getResponse() {
        return response;
    }
}
