package com.plummy.vio.script;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class ListenerScript extends AbstractToggleableScript {

    // The wrapped handlers on the event bus while the script is active, kept to unregister exactly those.
    private List<Consumer<?>> registered = List.of();
    // Handler calls off the server thread are skipped; this keeps that from being logged on every event.
    private final AtomicBoolean offThreadReported = new AtomicBoolean();

    public ListenerScript(ScriptFile file, CompiledScript compiled, String error) {
        super(file, compiled, error);
    }

    List<Consumer<?>> getRegistered() {
        return registered;
    }

    void setRegistered(List<Consumer<?>> registered) {
        this.registered = registered;
    }

    // True only the first time.
    boolean reportOffThread() {
        return offThreadReported.compareAndSet(false, true);
    }
}
