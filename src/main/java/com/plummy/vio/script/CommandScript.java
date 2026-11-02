package com.plummy.vio.script;

import com.plummy.vio.script.runtime.CommandRuntimeScript;
import com.plummy.vio.script.runtime.RuntimeScript;

public final class CommandScript implements Script, Toggleable {

    private final ScriptKey key;
    private final String description;
    private final boolean isEnabled;

    public CommandScript(ScriptKey key, String description, boolean isEnabled) {
        this.key = key;
        this.description = description;
        this.isEnabled = isEnabled;
    }

    @Override
    public ScriptKey getKey() {
        return key;
    }

    @Override
    public ScriptType getType() {
        return ScriptType.COMMAND;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Class<? extends RuntimeScript> getRuntimeBaseClass() {
        return CommandRuntimeScript.class;
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    @Override
    public CommandScript withEnabled(boolean isEnabled) {
        return new CommandScript(key, description, isEnabled);
    }

    @Override
    public <T> T accept(ScriptVisitor<T> visitor) {
        return visitor.visitCommandScript(this);
    }
}
