package com.plummy.vio.tool;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class ToolContext {

    private final ServerPlayer player;

    public ToolContext(ServerPlayer player) {
        this.player = player;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public MinecraftServer getServer() {
        return player.getServer();
    }

    public ServerLevel getLevel() {
        return player.serverLevel();
    }
}
