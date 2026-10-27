package com.plummy.vio.message;

import java.time.Instant;

public interface Message {

    Instant getDispatchTime();

    <T> T accept(MessageVisitor<T> visitor);
}
