package com.plummy.vio.message;

import com.plummy.vio.section.Section;

import java.util.List;

public interface Message {

    MessageType getType();

    List<Section> getSections();

    <T> T accept(MessageVisitor<T> visitor);
}
