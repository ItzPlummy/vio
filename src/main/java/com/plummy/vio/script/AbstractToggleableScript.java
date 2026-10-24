package com.plummy.vio.script;

public abstract class AbstractToggleableScript extends Script implements Toggleable {

    protected AbstractToggleableScript(ScriptFile file, CompiledScript compiled, String error) {
        super(file, compiled, error);
    }

    @Override
    public boolean isEnabled() {
        return getHeader().isEnabled();
    }
}
