package com.plummy.vio.client.message;

import java.time.Duration;

public interface ThoughtMessage extends ClientMessage {

    Duration getThoughtDuration();
}
