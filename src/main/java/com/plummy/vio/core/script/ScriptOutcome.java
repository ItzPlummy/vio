package com.plummy.vio.core.script;

public class ScriptOutcome {

    private final Object result;
    private final String error;
    private final Throwable exception;
    private final long durationMillis;

    public ScriptOutcome(Object result, String error, Throwable exception, long durationMillis) {
        this.result = result;
        this.error = error;
        this.exception = exception;
        this.durationMillis = durationMillis;
    }

    public Object getResult() {
        return result;
    }

    public String getError() {
        return error;
    }

    public Throwable getException() {
        return exception;
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    public boolean isSuccessful() {
        return error == null;
    }
}
