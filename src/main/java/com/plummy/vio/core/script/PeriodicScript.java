package com.plummy.vio.core.script;

public final class PeriodicScript extends AbstractToggleableScript implements ToggleableScript {

    PeriodicScript(String name, String description, String source) {
        super(name, description, source);
    }

    PeriodicScript(String name, String description, String source, boolean isEnabled) {
        super(name, description, source, isEnabled);
    }

    @Override
    public ScriptType getType() {
        return ScriptType.PERIODIC;
    }

    @Override
    public <V> V accept(ScriptVisitor<V> visitor) {
        return visitor.visit(this);
    }
}
