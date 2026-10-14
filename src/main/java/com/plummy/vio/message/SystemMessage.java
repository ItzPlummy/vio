package com.plummy.vio.message;

import com.plummy.vio.section.Section;
import com.plummy.vio.section.TextSection;

import java.util.List;

public class SystemMessage implements Message {

    private final TextSection text;

    public SystemMessage(TextSection text) {
        this.text = text;
    }

    public static SystemMessage of(String text) {
        return new SystemMessage(new TextSection(text));
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
