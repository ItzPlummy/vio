package com.plummy.vio.script;

// Thrown inside a script once its budget is used up. An Error, so a script's catch (Exception) can't swallow it, and
// it's thrown again at the next check anyway.
public class ScriptTimeoutError extends Error {

    public ScriptTimeoutError(String message) {
        super(message);
    }
}
