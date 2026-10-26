package com.plummy.vio.core.script;

import java.time.Instant;
import java.util.UUID;

public abstract class AbstractToggleableScript extends AbstractScript implements Toggleable {

    private boolean enabled;
    private UUID toggledBy;
    private Instant toggledAt;

    protected AbstractToggleableScript(String name, String description, UUID creator, Instant createdAt, boolean enabled, UUID toggledBy, Instant toggledAt, String source) {
        super(name, description, creator, createdAt, source);
        this.enabled = enabled;
        this.toggledBy = toggledBy;
        this.toggledAt = toggledAt;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled, UUID toggledBy, Instant toggledAt) {
        this.enabled = enabled;
        this.toggledBy = toggledBy;
        this.toggledAt = toggledAt;
    }

    @Override
    public UUID getToggledBy() {
        return toggledBy;
    }

    @Override
    public Instant getToggledAt() {
        return toggledAt;
    }

    @Override
    public ScriptState getState() {
        ScriptState state = super.getState();
        return state == ScriptState.ENABLED && !enabled ? ScriptState.DISABLED : state;
    }
}
