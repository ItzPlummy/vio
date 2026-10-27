package com.plummy.vio.script;

import com.plummy.vio.script.dsl.CompiledScript;
import groovy.lang.GroovyClassLoader;

import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.UUID;

public abstract class AbstractScript implements Script {

    private final String name;
    private final String description;
    private final UUID creator;
    private final Instant createdAt;
    private final String source;
    private final ScriptLog log = new ScriptLog();
    private boolean compileAttempted;
    private GroovyClassLoader loader;
    private Class<? extends CompiledScript> scriptClass;
    private String error;
    private int failures;

    protected AbstractScript(String name, String description, UUID creator, Instant createdAt, String source) {
        this.name = name;
        this.description = description;
        this.creator = creator;
        this.createdAt = createdAt;
        this.source = source;
    }

    @Override
    public String getKey() {
        return getName();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public ScriptState getState() {
        return error == null ? ScriptState.ENABLED : ScriptState.ERRORED;
    }

    @Override
    public UUID getCreator() {
        return creator;
    }

    @Override
    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public String getSource() {
        return source;
    }

    @Override
    public void compile() {
        if (compileAttempted) {
            throw new IllegalStateException("Script %s was compiled already".formatted(name));
        }

        compileAttempted = true;

        GroovyClassLoader loader = ScriptCompiler.createLoader(getType());

        try {
            scriptClass = ScriptCompiler.compile(loader, name, source);
            this.loader = loader;
        } catch (ScriptException e) {
            ScriptCompiler.dispose(loader);
            error = e.getMessage();
        }
    }

    @Override
    public boolean isCompiled() {
        return scriptClass != null;
    }

    @Override
    public CompiledScript newInstance() {
        if (scriptClass == null) {
            throw new IllegalStateException("Script %s isn't compiled".formatted(name));
        }

        try {
            return scriptClass.getDeclaredConstructor().newInstance();
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException cause) {
                throw cause;
            }

            if (e.getCause() instanceof Error cause) {
                throw cause;
            }

            throw new IllegalStateException("Failed to create an instance of %s".formatted(scriptClass.getName()), e);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to create an instance of %s".formatted(scriptClass.getName()), e);
        }
    }

    @Override
    public ScriptLog getLog() {
        return log;
    }

    @Override
    public String getError() {
        return error;
    }

    @Override
    public void setError(String error) {
        this.error = error;
    }

    @Override
    public int recordFailure() {
        return ++failures;
    }

    @Override
    public void resetFailures() {
        failures = 0;
    }

    @Override
    public void dispose() {
        if (loader != null) {
            ScriptCompiler.dispose(loader);
            loader = null;
            scriptClass = null;
        }
    }
}
