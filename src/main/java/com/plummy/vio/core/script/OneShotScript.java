package com.plummy.vio.core.script;

public final class OneShotScript extends AbstractScript implements RunnableScript {

    public OneShotScript(String name, String description, String source) {
        super(name, description, source);
    }

    @Override
    public ScriptType getType() {
        return ScriptType.ONE_SHOT;
    }

    @Override
    public <V> V accept(ScriptVisitor<V> visitor) {
        return visitor.visit(this);
    }
}
