package com.plummy.vio.tool.catalog.player;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.plummy.vio.access.AccessLevel;
import com.plummy.vio.tool.ServerTool;
import com.plummy.vio.tool.executor.ExecutionResult;
import com.plummy.vio.tool.value.argument.ArgumentDefinition;
import com.plummy.vio.tool.value.argument.ArgumentType;
import com.plummy.vio.tool.value.argument.Arguments;
import com.plummy.vio.tool.value.catalog.Page;
import com.plummy.vio.tool.value.result.ResultValueJsonSerializer;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.time.Instant;
import java.util.*;

public class GetAdvancementsTool implements ServerTool {

    private static final int MAX_LIMIT = 50;
    private static final int RECENT = 5;

    @Override
    public String getName() {
        return "get_advancements";
    }

    @Override
    public String getDescription() {
        return "Summary of a player's advancements: done/total, the most recent ones, and the unfinished ones closest to completion. Use get_advancement_progress for one advancement's criteria. Recipe unlocks and hidden technical advancements are excluded.";
    }

    @Override
    public SequencedMap<String, ArgumentDefinition> getArgumentSchema() {
        SequencedMap<String, ArgumentDefinition> schema = new LinkedHashMap<>();

        schema.put("status", ArgumentDefinition.status());
        schema.put("player", new ArgumentDefinition(ArgumentType.PLAYER_REFERENCE, "Online player name."));
        schema.put("page", ArgumentDefinition.page(MAX_LIMIT, 10, "closest unfinished advancements"));

        return schema;
    }

    @Override
    public AccessLevel getAccessLevel() {
        return AccessLevel.BASIC;
    }

    @Override
    public ExecutionResult execute(Context context, Arguments arguments) {
        MinecraftServer server = context.getServer();

        ServerPlayer player = context.getPlayer(arguments.getPlayerReference("player"));
        Page page = arguments.getPage("page", MAX_LIMIT);

        Map<ResourceLocation, AdvancementProgress> progress = getProgress(player);
        List<AdvancementHolder> visible = getVisible(server);

        List<AdvancementHolder> done = visible.stream().filter(holder -> progress.get(holder.id()).isDone()).toList();
        List<AdvancementHolder> unfinished = visible.stream()
                .filter(holder -> !progress.get(holder.id()).isDone())
                .sorted(Comparator.<AdvancementHolder>comparingInt(holder -> countRemaining(holder, progress.get(holder.id()))).thenComparing(holder -> holder.id().toString()))
                .toList();

        List<String> recent = done.stream()
                .sorted(Comparator.<AdvancementHolder, Instant>comparing(holder -> getLastObtained(progress.get(holder.id())), Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(holder -> holder.id().toString()))
                .limit(RECENT)
                .map(holder -> holder.id().toString())
                .toList();

        JsonArray closest = new JsonArray();

        for (AdvancementHolder holder : page.apply(unfinished)) {
            JsonObject entry = new JsonObject();

            entry.addProperty("id", holder.id().toString());
            entry.addProperty("title", holder.value().display().map(display -> display.getTitle().getString()).orElse(null));
            entry.addProperty("criteria_remaining", countRemaining(holder, progress.get(holder.id())));

            closest.add(entry);
        }

        JsonObject json = new JsonObject();

        json.addProperty("player", player.getGameProfile().getName());
        json.addProperty("done", done.size());
        json.addProperty("total_advancements", visible.size());
        json.add("recent", recent.stream().collect(JsonArray::new, JsonArray::add, JsonArray::addAll));
        json.add("closest", closest);
        json.add("page", page.withTotal(unfinished.size()).accept(ResultValueJsonSerializer.getInstance()));

        return ExecutionResult.success(json);
    }

    private static Map<ResourceLocation, AdvancementProgress> getProgress(ServerPlayer player) {
        Map<ResourceLocation, AdvancementProgress> progress = new HashMap<>();

        for (AdvancementHolder holder : player.server.getAdvancements().getAllAdvancements()) {
            progress.put(holder.id(), player.getAdvancements().getOrStartProgress(holder));
        }

        return progress;
    }

    static List<AdvancementHolder> getVisible(MinecraftServer server) {
        return server.getAdvancements().getAllAdvancements().stream()
                .filter(holder -> holder.value().display().isPresent() && !holder.id().getPath().startsWith("recipes/"))
                .sorted(Comparator.comparing(holder -> holder.id().toString()))
                .toList();
    }

    private static int countRemaining(AdvancementHolder holder, AdvancementProgress progress) {
        int remaining = 0;

        for (List<String> group : holder.value().requirements().requirements()) {
            boolean met = group.stream().anyMatch(criterion -> {
                CriterionProgress criterionProgress = progress.getCriterion(criterion);
                return criterionProgress != null && criterionProgress.isDone();
            });

            if (!met) {
                remaining++;
            }
        }

        return remaining;
    }

    private static Instant getLastObtained(AdvancementProgress progress) {
        Instant last = null;

        for (String criterion : progress.getCompletedCriteria()) {
            CriterionProgress criterionProgress = progress.getCriterion(criterion);

            if (criterionProgress != null && criterionProgress.getObtained() != null && (last == null || criterionProgress.getObtained().isAfter(last))) {
                last = criterionProgress.getObtained();
            }
        }

        return last;
    }
}
