package com.plummy.vio.tool.catalog.datapack;

import com.google.gson.JsonObject;
import com.plummy.vio.Vio;
import com.plummy.vio.access.AccessLevel;
import com.plummy.vio.config.DatapackLimits;
import com.plummy.vio.tool.BasicTool;
import com.plummy.vio.tool.executor.ExecutionResult;
import com.plummy.vio.tool.requester.ToolRequester;
import com.plummy.vio.tool.value.argument.ArgumentDefinition;
import com.plummy.vio.tool.value.argument.Arguments;

import java.util.LinkedHashMap;
import java.util.SequencedMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ApplyDatapackChangesTool implements BasicTool {

    @Override
    public String getName() {
        return "apply_datapack_changes";
    }

    @Override
    public String getDescription() {
        return "Reload the server's data, like /reload, so every change written to Vio's data pack since the last reload takes effect: recipes, loot tables, advancements, predicates, item modifiers, functions, tags, data maps and global loot modifiers. "
                + "Changes that need a restart stay pending until the server restarts. Call it once after writing a whole change, never after each file: it lags the server and resends recipes and tags to every player. "
                + "Success means the reload finished; a file the game still can't load is skipped and logged, which read_server_log shows.";
    }

    @Override
    public SequencedMap<String, ArgumentDefinition> getArgumentSchema() {
        SequencedMap<String, ArgumentDefinition> schema = new LinkedHashMap<>();

        schema.put("status", ArgumentDefinition.status());

        return schema;
    }

    @Override
    public AccessLevel getAccessLevel() {
        return AccessLevel.FULL;
    }

    @Override
    public boolean isAvailable() {
        return DatapackTools.isAvailable();
    }

    @Override
    public ExecutionResult execute(ToolRequester requester, Arguments arguments) {
        try {
            DatapackTools.getManager().apply().get(DatapackLimits.RELOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            return ExecutionResult.failure("The reload is taking longer than %s seconds. It keeps going, so check again in a moment".formatted(DatapackLimits.RELOAD_TIMEOUT_SECONDS));
        } catch (ExecutionException e) {
            Vio.getLogger().warn("Failed to reload data packs", e.getCause());
            return ExecutionResult.failure("The reload failed, so the server kept its previous data: %s".formatted(e.getCause()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ExecutionResult.failure("Reload interrupted");
        }

        JsonObject json = new JsonObject();

        json.addProperty("applied", true);

        return ExecutionResult.success(json);
    }
}
