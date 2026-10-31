package com.plummy.vio.script;

import java.util.List;

public record ScriptOutcome(Object result, String error, Throwable exception, List<ScriptLogEntry> output, long durationMillis) {

    public boolean isSuccessful() {
        return error == null;
    }

    public ScriptOutcome withOutput(List<ScriptLogEntry> output) {
        return new ScriptOutcome(result, error, exception, output, durationMillis);
    }
}
