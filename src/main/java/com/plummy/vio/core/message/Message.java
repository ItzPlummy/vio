package com.plummy.vio.core.message;

import com.plummy.vio.core.section.Section;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

public interface Message {

    MessageType getType();

    Instant getTime();

    List<Section> getSections();

    <T> T accept(MessageVisitor<T> visitor);
}
