package com.plummy.vio.message;

import com.plummy.vio.section.Section;
import com.plummy.vio.section.TextSection;

import java.util.List;

public class PlayerRequestMessage implements ConversationMessage {

    private final TextSection request;

    public PlayerRequestMessage(String content) {
        this(new TextSection(content));
    }

    public PlayerRequestMessage(TextSection request) {
        this.request = request;
    }

    @Override
    public MessageType getType() {
        return MessageType.PLAYER_REQUEST;
    }

    @Override
    public List<Section> getSections() {
        return List.of(
                getRequest()
        );
    }

    @Override
    public <T> T accept(MessageVisitor<T> visitor) {
        return visitor.visitPlayerRequestMessage(this);
    }

    public TextSection getRequest() {
        return request;
    }
}
