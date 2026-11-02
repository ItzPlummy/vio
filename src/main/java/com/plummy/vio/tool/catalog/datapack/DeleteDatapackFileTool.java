package com.plummy.vio.tool.catalog.datapack;

import com.google.gson.JsonObject;
import com.plummy.vio.access.AccessLevel;
import com.plummy.vio.datapack.DatapackException;
import com.plummy.vio.datapack.DatapackPath;
import com.plummy.vio.tool.ServerTool;
import com.plummy.vio.tool.executor.ExecutionResult;
import com.plummy.vio.tool.value.argument.ArgumentDefinition;
import com.plummy.vio.tool.value.argument.ArgumentType;
import com.plummy.vio.tool.value.argument.Arguments;

import java.util.LinkedHashMap;
import java.util.SequencedMap;

public class DeleteDatapackFileTool implements ServerTool {

    @Override
    public String getName() {
        return "delete_datapack_file";
    }

    @Override
    public String getDescription() {
        return "Delete a file from Vio's data pack for good; there's no undo, so read it first if it may be needed again. Like writing, it applies nothing until apply_datapack_changes, or a restart when needs_restart is true. "
                + "Removing registry content like an enchantment or a biome that the world already uses can break items or chunks that refer to it.";
    }

    @Override
    public SequencedMap<String, ArgumentDefinition> getArgumentSchema() {
        SequencedMap<String, ArgumentDefinition> schema = new LinkedHashMap<>();

        schema.put("status", ArgumentDefinition.status());
        schema.put("path", new ArgumentDefinition(ArgumentType.STRING, DatapackTools.PATH_DESCRIPTION));

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
        boolean needsRestart;

        try {
            needsRestart = DatapackTools.getManager().deleteFile(path);
        } catch (DatapackException e) {
            return ExecutionResult.invalidArguments(e.getMessage());
        }

        JsonObject json = new JsonObject();

        json.addProperty("path", path.toString());
        json.addProperty("deleted", true);
        json.addProperty("needs_restart", needsRestart);

        return ExecutionResult.success(json);
    }
}
