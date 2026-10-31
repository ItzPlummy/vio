package com.plummy.vio.script;

import java.time.Instant;

public record ScriptLogEntry(String message, Instant dispatchTime, ScriptLogLevel level) {
}
