package com.plummy.vio.script;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class ListenerScript extends AbstractToggleableScript {

    private List<Consumer<?>> registered = List.of();

    public ListenerScript(String name, String description, UUID creator, Instant createdAt, boolean enabled, UUID toggledBy, Instant toggledAt, String source) {
        super(name, description, creator, createdAt, enabled, toggledBy, toggledAt, source);
    }

    @Override
    public ScriptType getType() {
        return ScriptType.LISTENER;
    }

    @Override
    public <T> T accept(ScriptVisitor<T> visitor) {
        return visitor.visitListenerScript(this);
    }

    List<Consumer<?>> getRegistered() {
        return registered;
    }

    void setRegistered(List<Consumer<?>> registered) {
        this.registered = registered;
    }
}
