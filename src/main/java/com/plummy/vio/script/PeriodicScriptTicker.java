package com.plummy.vio.script;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public class PeriodicScriptTicker {

    private final ScriptManager scriptManager;

    public PeriodicScriptTicker(ScriptManager scriptManager) {
        this.scriptManager = scriptManager;
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        scriptManager.tick();
    }
}
