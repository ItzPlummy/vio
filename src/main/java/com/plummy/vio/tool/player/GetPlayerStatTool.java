package com.plummy.vio.tool.player;

import com.google.gson.JsonObject;
import com.plummy.vio.access.AccessLevel;
import com.plummy.vio.tool.ExecutionResult;
import com.plummy.vio.tool.ServerTool;
import com.plummy.vio.tool.argument.ArgumentDefinition;
import com.plummy.vio.tool.argument.ArgumentType;
import com.plummy.vio.tool.argument.Arguments;
import com.plummy.vio.tool.argument.Identifier;
import net.minecraft.core.Registry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.StatType;

import java.util.LinkedHashMap;
import java.util.SequencedMap;

public class GetPlayerStatTool implements ServerTool {

    @Override
    public String getName() {
        return "get_player_stat";
    }

    @Override
    public String getDescription() {
        return "One statistic of a player by id, with units converted like get_player_stats.";
    }

    @Override
    public SequencedMap<String, ArgumentDefinition> getArgumentSchema() {
        SequencedMap<String, ArgumentDefinition> schema = new LinkedHashMap<>();

        schema.put("player", new ArgumentDefinition(ArgumentType.PLAYER_REFERENCE, "Online player name."));
        schema.put("category", ArgumentDefinition.enumeration(GetPlayerStatsTool.Category.class, "custom = general stats (play time, distances, deaths, jumps, ...); mined is per block; crafted, used, broken, picked_up and dropped are per item; killed and killed_by are per entity type."));
        schema.put("stat", new ArgumentDefinition(ArgumentType.IDENTIFIER, "Stat id within the category, e.g. minecraft:play_time (custom) or minecraft:diamond_ore (mined)."));

        return schema;
    }

    @Override
    public AccessLevel getAccessLevel() {
        return AccessLevel.BASIC;
    }

    @Override
    public ExecutionResult execute(Context context, Arguments arguments) {
        ServerPlayer player = context.getPlayer(arguments.getPlayerReference("player"));
        GetPlayerStatsTool.Category category = arguments.getEnum("category", GetPlayerStatsTool.Category.class);
        ServerStatsCounter counter = player.getStats();
        GetPlayerStatsTool.Entry entry = read(context, counter, category.getType(), category == GetPlayerStatsTool.Category.CUSTOM, arguments.getIdentifier("stat"));

        JsonObject json = new JsonObject();

        json.addProperty("player", player.getGameProfile().getName());
        json.addProperty("category", ArgumentDefinition.getEnumValue(category));
        json.addProperty("id", entry.id());
        json.addProperty("value", entry.value());
        json.addProperty("unit", entry.unit());

        return ExecutionResult.success(json);
    }

    private static <T> GetPlayerStatsTool.Entry read(Context context, ServerStatsCounter counter, StatType<T> type, boolean custom, Identifier stat) {
        Registry<T> registry = type.getRegistry();
        T value = context.getEntry(registry, stat, "stat").value();

        return GetPlayerStatsTool.toEntry(registry.getKey(value), counter.getValue(type.get(value)), custom);
    }
}
