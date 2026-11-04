package com.plummy.vio.core.script;

abstract class AbstractToggleableScript extends AbstractScript {

    private boolean isEnabled;

    AbstractToggleableScript(String name, String description, String source) {
        super(name, description, source);
        this.isEnabled = true;
    }

    AbstractToggleableScript(String name, String description, String source, boolean isEnabled) {
        super(name, description, source);
        this.isEnabled = isEnabled;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void enable() {
        isEnabled = true;
    }

    public void disable() {
        isEnabled = false;
    }
}
