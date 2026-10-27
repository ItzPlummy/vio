package com.plummy.vio.message;

import com.plummy.vio.section.Section;

import java.time.Instant;
import java.util.List;

public interface Message {

    MessageType getType();

    Instant getTime();

    List<Section> getSections();

    <T> T accept(MessageVisitor<T> visitor);
}
