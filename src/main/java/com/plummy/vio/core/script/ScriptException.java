package com.plummy.vio.core.script;

// A script that can't be used, such as one that doesn't compile. The message is meant for the model to fix it.
public class ScriptException extends RuntimeException {

    public ScriptException(String message) {
        super(message);
    }

    public ScriptException(String message, Throwable cause) {
        super(message, cause);
    }
}
