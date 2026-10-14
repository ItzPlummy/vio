package com.plummy.vio.message;

import com.plummy.vio.section.Section;
import com.plummy.vio.section.TextSection;

import java.util.List;

public class SystemMessage implements Message {

    private final TextSection text;

    public SystemMessage(String content) {
        this(new TextSection(content));
    }

    public SystemMessage(TextSection section) {
        this.text = section;
    }

    @Override
    public MessageType getType() {
        return MessageType.SYSTEM;
    }

    @Override
    public List<Section> getSections() {
        return List.of(
                getText()
        );
    }

    @Override
    public <T> T accept(MessageVisitor<T> visitor) {
        return visitor.visitSystemMessage(this);
    }

    public TextSection getText() {
        return text;
    }
}
