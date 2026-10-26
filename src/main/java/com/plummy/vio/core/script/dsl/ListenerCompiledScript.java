package com.plummy.vio.core.script.dsl;

import net.neoforged.bus.api.Event;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public abstract class ListenerCompiledScript extends CompiledScript {

    private final Map<Class<? extends Event>, Collection<Consumer<Event>>> handlers = new LinkedHashMap<>();

    public <T extends Event> void on(Class<T> type, Consumer<T> handler) {
        if (type == null || handler == null) {
            throw new IllegalArgumentException("on(...) needs an event class and a handler");
        }

        handlers.computeIfAbsent(type, key -> new ArrayList<>()).add(event -> handler.accept(type.cast(event)));
    }

    public Map<Class<? extends Event>, Collection<Consumer<Event>>> getHandlers() {
        return handlers;
    }
}
