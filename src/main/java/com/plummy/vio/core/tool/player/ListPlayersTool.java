package com.plummy.vio.core.tool.player;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.plummy.vio.core.access.AccessLevel;
import com.plummy.vio.core.tool.ExecutionResult;
import com.plummy.vio.core.tool.ServerTool;
import com.plummy.vio.core.tool.argument.ArgumentDefinition;
import com.plummy.vio.core.tool.argument.ArgumentType;
import com.plummy.vio.core.tool.argument.Arguments;
import com.plummy.vio.core.tool.argument.Position;
import com.plummy.vio.core.tool.result.ResultValueJsonSerializer;
import net.minecraft.Util;
import net.minecraft.server.level.ServerPlayer;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;

public class ListPlayersTool implements ServerTool {

    @Override
    public String getName() {
        return "list_players";
    }

    @Override
    public String getDescription() {
        return "Online players with dimension, game mode, health, ping and idle time, optionally with positions and distance from the requesting player (null across dimensions).";
    }

    @Override
    public SequencedMap<String, ArgumentDefinition> getArgumentSchema() {
        SequencedMap<String, ArgumentDefinition> schema = new LinkedHashMap<>();

        schema.put("include_positions", new ArgumentDefinition(ArgumentType.BOOLEAN, "Whether to include positions and distances."));

        return schema;
    }

    @Override
    public AccessLevel getAccessLevel() {
        return AccessLevel.BASIC;
    }

    @Override
    public ExecutionResult execute(Context context, Arguments arguments) {
        boolean includePositions = arguments.getBoolean("include_positions");
        ServerPlayer requester = context.getPlayer();

        List<ServerPlayer> players = context.getServer().getPlayerList().getPlayers().stream()
                .sorted(Comparator.comparing((ServerPlayer player) -> player.getGameProfile().getName()).thenComparing(ServerPlayer::getUUID))
                .toList();

        JsonArray results = new JsonArray();

        for (ServerPlayer player : players) {
            JsonObject json = new JsonObject();

            json.addProperty("name", player.getGameProfile().getName());
            json.addProperty("uuid", player.getStringUUID());
            json.addProperty("dimension", player.level().dimension().location().toString());

            if (includePositions) {
                json.add("pos", Position.of(player.position()).accept(ResultValueJsonSerializer.getInstance()));

                if (player.level() == requester.level()) {
                    json.addProperty("distance_blocks", Math.round(player.position().distanceTo(requester.position()) * 10) / 10.0);
                } else {
                    json.add("distance_blocks", null);
                }
            }

            json.addProperty("gamemode", player.gameMode.getGameModeForPlayer().getName());
            json.addProperty("health", Math.round(player.getHealth() * 10) / 10.0);
            json.addProperty("ping_ms", player.connection.latency());
            json.addProperty("idle_seconds", (Util.getMillis() - player.getLastActionTime()) / 1000);

            results.add(json);
        }

        JsonObject json = new JsonObject();
        json.add("players", results);

        return ExecutionResult.success(json);
    }
}
