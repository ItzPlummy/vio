package com.plummy.vio.core.tool.catalog.player;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.plummy.vio.core.access.AccessLevel;
import com.plummy.vio.core.tool.executor.ExecutionResult;
import com.plummy.vio.core.tool.ServerTool;
import com.plummy.vio.core.tool.value.argument.ArgumentDefinition;
import com.plummy.vio.core.tool.value.argument.ArgumentType;
import com.plummy.vio.core.tool.value.argument.Arguments;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;

public class GetAdvancementProgressTool implements ServerTool {

    @Override
    public String getName() {
        return "get_advancement_progress";
    }

    @Override
    public String getDescription() {
        return "One advancement of a player: whether it's done, and its done and remaining criteria.";
    }

    @Override
    public SequencedMap<String, ArgumentDefinition> getArgumentSchema() {
        SequencedMap<String, ArgumentDefinition> schema = new LinkedHashMap<>();

        schema.put("status", ArgumentDefinition.status());
        schema.put("player", new ArgumentDefinition(ArgumentType.PLAYER_REFERENCE, "Online player name."));
        schema.put("advancement", new ArgumentDefinition(ArgumentType.IDENTIFIER, "Advancement id, e.g. minecraft:adventure/kill_all_mobs."));

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
        ResourceLocation location = arguments.getIdentifier("advancement").getId();
        List<AdvancementHolder> visible = GetAdvancementsTool.getVisible(server);

        AdvancementHolder holder = visible.stream().filter(candidate -> candidate.id().equals(location)).findFirst().orElse(null);

        if (holder == null) {
            return ExecutionResult.invalidArguments("Unknown advancement %s".formatted(location));
        }

        AdvancementProgress advancementProgress = player.getAdvancements().getOrStartProgress(holder);
        DisplayInfo display = holder.value().display().orElseThrow();
        List<String> criteriaDone = new ArrayList<>();
        List<String> criteriaRemaining = new ArrayList<>();

        advancementProgress.getCompletedCriteria().forEach(criteriaDone::add);
        advancementProgress.getRemainingCriteria().forEach(criteriaRemaining::add);

        JsonObject json = new JsonObject();

        json.addProperty("player", player.getGameProfile().getName());
        json.addProperty("id", holder.id().toString());
        json.addProperty("title", display.getTitle().getString());
        json.addProperty("description", display.getDescription().getString());
        json.addProperty("done", advancementProgress.isDone());
        json.add("criteria_done", criteriaDone.stream().sorted().collect(JsonArray::new, JsonArray::add, JsonArray::addAll));
        json.add("criteria_remaining", criteriaRemaining.stream().sorted().collect(JsonArray::new, JsonArray::add, JsonArray::addAll));

        return ExecutionResult.success(json);
    }
}
