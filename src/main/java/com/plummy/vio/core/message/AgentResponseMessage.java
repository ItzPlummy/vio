package com.plummy.vio.core.message;

import com.plummy.vio.core.section.Section;
import com.plummy.vio.core.section.TextSection;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class AgentResponseMessage extends AbstractMessage {

    private final TextSection response;

    public AgentResponseMessage(TextSection response, Instant time) {
        super(time);
        this.response = response;
    }

    public static AgentResponseMessage of(String response) {
        return new AgentResponseMessage(
                new TextSection(response),
                Instant.now().truncatedTo(ChronoUnit.MILLIS)
        );
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
