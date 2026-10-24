package com.plummy.vio.script.dsl;

import net.neoforged.bus.api.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

// Base class of listener scripts. The body only declares handlers with on(...); Vio registers them on the game event
// bus while the script is enabled.
public abstract class ListenerVioScript extends VioScript {

    private final List<Handler<?>> handlers = new ArrayList<>();

    public <T extends Event> void on(Class<T> type, Consumer<T> handler) {
        handlers.add(new Handler<>(type, handler));
    }

    public List<Handler<?>> getHandlers() {
        return handlers;
    }

    public record Handler<T extends Event>(Class<T> type, Consumer<T> consumer) {
    }
}
