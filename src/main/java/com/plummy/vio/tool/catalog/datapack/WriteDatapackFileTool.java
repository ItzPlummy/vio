package com.plummy.vio.tool.catalog.datapack;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.plummy.vio.access.AccessLevel;
import com.plummy.vio.config.DatapackLimits;
import com.plummy.vio.datapack.DatapackException;
import com.plummy.vio.datapack.DatapackFile;
import com.plummy.vio.datapack.DatapackPath;
import com.plummy.vio.datapack.DatapackWriteResult;
import com.plummy.vio.tool.ServerTool;
import com.plummy.vio.tool.executor.ExecutionResult;
import com.plummy.vio.tool.value.argument.ArgumentDefinition;
import com.plummy.vio.tool.value.argument.ArgumentType;
import com.plummy.vio.tool.value.argument.Arguments;

import java.util.LinkedHashMap;
import java.util.SequencedMap;

public class WriteDatapackFileTool implements ServerTool {

    @Override
    public String getName() {
        return "write_datapack_file";
    }

    @Override
    public String getDescription() {
        return "Create or replace a file in Vio's data pack, shared by every FULL player, after checking it the way the game will load it: errors come back and nothing is saved. "
                + "References to things the server doesn't have yet, like a new item tag, come back as warnings and the file is saved: they're fine if those are written too before the change applies, and otherwise the game skips or breaks that part. "
                + "Writing applies nothing: write every file of a change, then call apply_datapack_changes once, since each reload lags the server and resends recipes and tags to every player. "
                + "This is 1.21.1: folders are singular (recipe/, loot_table/, advancement/, predicate/, item_modifier/, function/, tags/item/, tags/block/), and the plural ones are rejected. "
                + "Use the 'vio' namespace for new content, or 'minecraft' to override a vanilla file with the same path. "
                + "Recipes, loot tables, advancements, predicates, item modifiers, functions, tags, NeoForge data maps (data_maps/) and global loot modifiers (loot_modifiers/, listed in neoforge/loot_modifiers/global_loot_modifiers.json) apply on reload. "
                + "Registry content such as enchantment/, damage_type/, worldgen/, dimension/, dimension_type/, painting_variant/ or jukebox_song/ only loads when the server starts, so it needs a restart (needs_restart in the result): a broken one can stop the world from loading, so get it right and tell the player a restart is needed.";
    }

    @Override
    public SequencedMap<String, ArgumentDefinition> getArgumentSchema() {
        SequencedMap<String, ArgumentDefinition> schema = new LinkedHashMap<>();

        schema.put("status", ArgumentDefinition.status());
        schema.put("path", new ArgumentDefinition(ArgumentType.STRING, DatapackTools.PATH_DESCRIPTION));
        schema.put("content", new ArgumentDefinition(ArgumentType.STRING, "The whole file: JSON, or one command per line without a leading slash for .mcfunction files."));

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
    public ExecutionResult execute(Context context, Arguments arguments) {
        DatapackPath path = DatapackTools.getPath(arguments);
        String content = arguments.getString("content");

        if (content.isBlank() || content.length() > DatapackLimits.MAX_FILE_LENGTH) {
            return ExecutionResult.invalidArguments("content must be 1 to %s characters, got %s".formatted(DatapackLimits.MAX_FILE_LENGTH, content.length()));
        }

        DatapackWriteResult result;

        try {
            result = DatapackTools.getManager().writeFile(new DatapackFile(path, content));
        } catch (DatapackException e) {
            return ExecutionResult.invalidArguments("%s\nNothing was saved".formatted(e.getMessage()));
        }

        JsonArray warnings = new JsonArray();
        result.warnings().forEach(warnings::add);

        JsonObject json = new JsonObject();

        json.addProperty("path", path.toString());
        json.addProperty("saved", true);
        json.addProperty("needs_restart", result.needsRestart());
        json.add("warnings", warnings);

        return ExecutionResult.success(json);
    }
}
