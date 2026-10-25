package com.plummy.vio.core.script;

// A loaded script: its file, compiled class and runtime-only state.
public abstract class Script {

    private ScriptFile file;
    // Null when the script doesn't compile.
    private final CompiledScript compiled;
    private final ScriptLog log = new ScriptLog();
    // Why the script is ERRORED, or null.
    private String error;
    private int failures;

    protected Script(ScriptFile file, CompiledScript compiled, String error) {
        this.file = file;
        this.compiled = compiled;
        this.error = error;
    }

    public static Script create(ScriptFile file, CompiledScript compiled, String error) {
        return switch (file.getHeader().getType()) {
            case ONE_SHOT -> new OneShotScript(file, compiled, error);
            case PERIODIC -> new PeriodicScript(file, compiled, error);
            case LISTENER -> new ListenerScript(file, compiled, error);
            case COMMAND -> new CommandScript(file, compiled, error);
        };
    }

    public String getName() {
        return file.getName();
    }

    public ScriptType getType() {
        return getHeader().getType();
    }

    public ScriptFile getFile() {
        return file;
    }

    // Only after the new file is saved, e.g. when toggled.
    void setFile(ScriptFile file) {
        this.file = file;
    }

    public ScriptHeader getHeader() {
        return file.getHeader();
    }

    public String getSource() {
        return file.getSource();
    }

    public CompiledScript getCompiled() {
        return compiled;
    }

    public ScriptLog getLog() {
        return log;
    }

    public String getError() {
        return error;
    }

    void setError(String error) {
        this.error = error;
    }

    // Consecutive failures since the last success.
    int recordFailure() {
        return ++failures;
    }

    void resetFailures() {
        failures = 0;
    }

    public ScriptState getState() {
        if (error != null) {
            return ScriptState.ERRORED;
        }

        if (this instanceof Toggleable toggleable && !toggleable.isEnabled()) {
            return ScriptState.DISABLED;
        }

        return ScriptState.ENABLED;
    }

    public void close() {
        if (compiled != null) {
            compiled.close();
        }
    }
}
