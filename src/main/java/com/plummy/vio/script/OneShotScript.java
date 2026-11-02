package com.plummy.vio.script;

import com.plummy.vio.script.runtime.RuntimeScript;

public final class OneShotScript implements Script, Executable {

    private final ScriptKey key;
    private final String description;

    public OneShotScript(ScriptKey key, String description) {
        this.key = key;
        this.description = description;
    }

    @Override
    public ScriptKey getKey() {
        return key;
    }

    @Override
    public ScriptType getType() {
        return ScriptType.ONE_SHOT;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Class<? extends RuntimeScript> getRuntimeBaseClass() {
        return RuntimeScript.class;
    }

    @Override
    public <T> T accept(ScriptVisitor<T> visitor) {
        return visitor.visitOneShotScript(this);
    }
}
