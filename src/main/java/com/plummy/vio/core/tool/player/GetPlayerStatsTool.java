package com.plummy.vio.core.tool.player;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.plummy.vio.core.access.AccessLevel;
import com.plummy.vio.core.tool.ExecutionResult;
import com.plummy.vio.core.tool.ServerTool;
import com.plummy.vio.core.tool.argument.ArgumentDefinition;
import com.plummy.vio.core.tool.argument.ArgumentType;
import com.plummy.vio.core.tool.argument.Arguments;
import com.plummy.vio.core.tool.argument.Page;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.StatType;
import net.minecraft.stats.Stats;

import java.util.*;

public class GetPlayerStatsTool implements ServerTool {

    private static final int MAX_LIMIT = 100;

    // Custom stats counted in ticks, shown in hours.
    private static final Set<String> TIME_STATS = Set.of("play_time", "total_world_time", "time_since_death", "time_since_rest", "sneak_time");
    private static final double TICKS_PER_HOUR = 72000.0;

    @Override
    public String getName() {
        return "get_player_stats";
    }

    @Override
    public String getDescription() {
        return "A player's statistics in one category, highest values first; use get_player_stat for one stat. Units are converted: times in hours, *_one_cm distances in blocks, damage in health points (2 per heart).";
    }

    @Override
    public SequencedMap<String, ArgumentDefinition> getArgumentSchema() {
        SequencedMap<String, ArgumentDefinition> schema = new LinkedHashMap<>();

        schema.put("player", new ArgumentDefinition(ArgumentType.PLAYER_REFERENCE, "Online player name."));
        schema.put("category", ArgumentDefinition.enumeration(Category.class, "custom = general stats (play time, distances, deaths, jumps, ...); mined is per block; crafted, used, broken, picked_up and dropped are per item; killed and killed_by are per entity type."));
        schema.put("limit", new ArgumentDefinition(ArgumentType.INTEGER, "Page size, 1 to 100; 10 is a good default."));
        schema.put("offset", new ArgumentDefinition(ArgumentType.INTEGER, "Entries to skip; 0 for the first page."));

        return schema;
    }

    @Override
    public AccessLevel getAccessLevel() {
        return AccessLevel.BASIC;
    }

    @Override
    public ExecutionResult execute(Context context, Arguments arguments) {
        ServerPlayer player = context.getPlayer(arguments.getPlayerReference("player"));
        Category category = arguments.getEnum("category", Category.class);
        Page page = arguments.getPage(MAX_LIMIT);
        ServerStatsCounter counter = player.getStats();

        List<Entry> entries = collect(counter, category.getType(), category == Category.CUSTOM);

        JsonArray stats = new JsonArray();

        for (Entry entry : page.apply(entries)) {
            JsonObject json = new JsonObject();

            json.addProperty("id", entry.id());
            json.addProperty("value", entry.value());
            json.addProperty("unit", entry.unit());

            stats.add(json);
        }

        JsonObject json = new JsonObject();

        json.addProperty("player", player.getGameProfile().getName());
        json.addProperty("category", ArgumentDefinition.getEnumValue(category));
        json.add("stats", stats);
        page.write(json, entries.size());

        return ExecutionResult.success(json);
    }

    private static <T> List<Entry> collect(ServerStatsCounter counter, StatType<T> type, boolean custom) {
        Registry<T> registry = type.getRegistry();

        List<Entry> entries = new ArrayList<>();

        for (T value : registry) {
            int raw = counter.getValue(type.get(value));

            if (raw != 0) {
                entries.add(toEntry(registry.getKey(value), raw, custom));
            }
        }

        entries.sort(Comparator.comparingDouble(Entry::value).reversed().thenComparing(Entry::id));

        return entries;
    }

    static Entry toEntry(ResourceLocation id, int raw, boolean custom) {
        if (custom) {
            String path = id.getPath();

            if (TIME_STATS.contains(path)) {
                return new Entry(id.toString(), Math.round(raw / TICKS_PER_HOUR * 100) / 100.0, "hours");
            }

            if (path.endsWith("_one_cm")) {
                return new Entry(id.toString(), Math.round(raw / 10.0) / 10.0, "blocks");
            }

            // Damage stats are stored in tenths of a health point.
            if (path.startsWith("damage_")) {
                return new Entry(id.toString(), raw / 10.0, "health_points");
            }
        }

        return new Entry(id.toString(), raw, "count");
    }

    enum Category {
        CUSTOM(Stats.CUSTOM),
        MINED(Stats.BLOCK_MINED),
        CRAFTED(Stats.ITEM_CRAFTED),
        USED(Stats.ITEM_USED),
        BROKEN(Stats.ITEM_BROKEN),
        PICKED_UP(Stats.ITEM_PICKED_UP),
        DROPPED(Stats.ITEM_DROPPED),
        KILLED(Stats.ENTITY_KILLED),
        KILLED_BY(Stats.ENTITY_KILLED_BY);

        private final StatType<?> type;

        Category(StatType<?> type) {
            this.type = type;
        }

        StatType<?> getType() {
            return type;
        }
    }

    record Entry(String id, double value, String unit) {
    }
}
