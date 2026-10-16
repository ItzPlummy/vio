package com.plummy.vio.message;

import com.plummy.vio.section.Section;
import com.plummy.vio.section.TextSection;

import java.time.Instant;
import java.util.List;

public class AgentResponseMessage implements Message {

    private final TextSection response;
    private final Instant time;

    public AgentResponseMessage(TextSection response, Instant time) {
        this.response = response;
        this.time = time;
    }

    public static AgentResponseMessage of(String response) {
        return new AgentResponseMessage(new TextSection(response), Message.now());
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

    @Override
    public Instant getTime() {
        return time;
    }

    public TextSection getResponse() {
        return response;
    }
}
