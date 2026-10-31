package com.plummy.vio.script;

import com.plummy.vio.script.runtime.ListenerRuntimeScript;
import com.plummy.vio.script.runtime.RuntimeScript;

public final class ListenerScript implements Script, Toggleable {

    private final ScriptKey key;
    private final String description;
    private final boolean isEnabled;

    public ListenerScript(ScriptKey key, String description, boolean isEnabled) {
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
        return ScriptType.LISTENER;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Class<? extends RuntimeScript> getRuntimeBaseClass() {
        return ListenerRuntimeScript.class;
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    @Override
    public ListenerScript withEnabled(boolean isEnabled) {
        return new ListenerScript(key, description, isEnabled);
    }

    @Override
    public <T> T accept(ScriptVisitor<T> visitor) {
        return visitor.visitListenerScript(this);
    }
}
