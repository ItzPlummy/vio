package com.plummy.vio.message;

import com.plummy.vio.registry.VisitorRegistry;
import com.plummy.vio.section.Section;

import java.util.List;
import java.util.stream.Collectors;

public interface Message {

    MessageType getType();

    List<Section> getSections();

    <T> T accept(MessageVisitor<T> visitor);

    default String getContent() {
        return getSections().stream()
                .map(section -> section.accept(VisitorRegistry.SECTION_CONTENT_SERIALIZER))
                .collect(Collectors.joining("\n\n"));
    }
}
