package com.plummy.vio.core.script;

public final class ListenerScript extends AbstractToggleableScript implements ToggleableScript {

    ListenerScript(String name, String description, String source) {
        super(name, description, source);
    }

    ListenerScript(String name, String description, String source, boolean isEnabled) {
        super(name, description, source, isEnabled);
    }

    @Override
    public ScriptType getType() {
        return ScriptType.LISTENER;
    }

    @Override
    public <V> V accept(ScriptVisitor<V> visitor) {
        return visitor.visit(this);
    }
}
