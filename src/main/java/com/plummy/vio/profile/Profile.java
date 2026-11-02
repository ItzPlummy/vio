package com.plummy.vio.profile;

import com.plummy.vio.access.AccessLevel;
import com.plummy.vio.script.Script;
import com.plummy.vio.script.ScriptManager;
import com.plummy.vio.script.ScriptState;
import com.plummy.vio.script.ScriptType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class Profile {

    private final PlayerInfo playerInfo;
    private final ServerInfo serverInfo;
    private final ScriptInfo scriptInfo;

    public Profile(PlayerInfo playerInfo, ServerInfo serverInfo, ScriptInfo scriptInfo) {
        this.playerInfo = playerInfo;
        this.serverInfo = serverInfo;
        this.scriptInfo = scriptInfo;
    }

    public static Profile of(ServerPlayer player, MinecraftServer server, AccessLevel accessLevel, ScriptInfo scriptInfo) {
        return new Profile(PlayerInfo.of(player, accessLevel), ServerInfo.of(server), scriptInfo);
    }

    public PlayerInfo getPlayerInfo() {
        return playerInfo;
    }

    public ServerInfo getServerInfo() {
        return serverInfo;
    }

    public ScriptInfo getScriptInfo() {
        return scriptInfo;
    }

    public record PlayerInfo(UUID id, String name, AccessLevel accessLevel, int level, String gameMode) {

        public static PlayerInfo of(ServerPlayer player, AccessLevel accessLevel) {
            return new PlayerInfo(player.getUUID(), player.getName().getString(), accessLevel, player.experienceLevel, player.gameMode.getGameModeForPlayer().getLongDisplayName().getString());
        }
    }

    public record ServerInfo(List<String> dimensions) {

        public static ServerInfo of(MinecraftServer server) {
            List<String> dimensions = new ArrayList<>();

            for (ServerLevel level : server.getAllLevels()) {
                dimensions.add(level.dimension().location().toString());
            }

            return new ServerInfo(dimensions);
        }
    }

    public record ScriptInfo(List<Entry> scripts) {

        public static ScriptInfo of(ScriptManager manager) {
            List<Entry> scripts = new ArrayList<>();

            for (Script script : manager.getScripts()) {
                scripts.add(new Entry(script.getKey().getKey(), script.getType(), manager.getScriptState(script.getKey()), script.getDescription()));
            }

            scripts.sort(Comparator.comparing(Entry::name));

            return new ScriptInfo(scripts);
        }

        public record Entry(String name, ScriptType type, ScriptState state, String description) {
        }
    }
}
