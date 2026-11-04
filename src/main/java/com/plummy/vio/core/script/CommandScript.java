package com.plummy.vio.core.script;

public final class CommandScript extends AbstractToggleableScript implements ToggleableScript {

    CommandScript(String name, String description, String source) {
        super(name, description, source);
    }

    CommandScript(String name, String description, String source, boolean isEnabled) {
        super(name, description, source, isEnabled);
    }

    @Override
    public ScriptType getType() {
        return ScriptType.COMMAND;
    }

    @Override
    public <V> V accept(ScriptVisitor<V> visitor) {
        return visitor.visit(this);
    }
}
