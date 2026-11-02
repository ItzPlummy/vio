package com.plummy.vio.tool.catalog.datapack;

import com.google.gson.JsonObject;
import com.plummy.vio.access.AccessLevel;
import com.plummy.vio.datapack.DatapackFile;
import com.plummy.vio.datapack.DatapackManager;
import com.plummy.vio.datapack.DatapackPath;
import com.plummy.vio.datapack.GameDatapackFile;
import com.plummy.vio.tool.ServerTool;
import com.plummy.vio.tool.executor.ExecutionResult;
import com.plummy.vio.tool.value.argument.ArgumentDefinition;
import com.plummy.vio.tool.value.argument.ArgumentType;
import com.plummy.vio.tool.value.argument.Arguments;

import java.util.LinkedHashMap;
import java.util.SequencedMap;

public class ReadDatapackFileTool implements ServerTool {

    @Override
    public String getName() {
        return "read_datapack_file";
    }

    @Override
    public String getDescription() {
        return ("A file's contents, null if there's no such file. Source 'vio' reads Vio's own data pack. Source 'game' reads the version the game loaded last, from whichever pack wins: vanilla, mods, other data packs or Vio's, and also says which pack it came from. "
                + "Formats changed a lot in 1.21, so before writing a file you're unsure of, read a similar vanilla one with source 'game', e.g. minecraft/recipe/stick.json or minecraft/loot_table/blocks/oak_log.json.%s").formatted(DatapackTools.DATA_NOTE);
    }

    @Override
    public SequencedMap<String, ArgumentDefinition> getArgumentSchema() {
        SequencedMap<String, ArgumentDefinition> schema = new LinkedHashMap<>();

        schema.put("status", ArgumentDefinition.status());
        schema.put("path", new ArgumentDefinition(ArgumentType.STRING, DatapackTools.PATH_DESCRIPTION));
        schema.put("source", ArgumentDefinition.enumeration(DatapackFileSource.class, "Where to read from: 'vio' for Vio's data pack, 'game' for the data the game loaded."));

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
        DatapackFileSource source = arguments.getEnum("source", DatapackFileSource.class);
        DatapackManager manager = DatapackTools.getManager();
        JsonObject json = new JsonObject();

        json.addProperty("path", path.toString());
        json.addProperty("source", ArgumentDefinition.getEnumValue(source));

        switch (source) {
            case VIO -> {
                DatapackFile file = manager.getFile(path);

                json.addProperty("content", file == null ? null : file.getContent());
            }
            case GAME -> {
                GameDatapackFile file = manager.getGameFile(path);

                json.addProperty("content", file == null ? null : file.content());
                json.addProperty("pack", file == null ? null : file.packId());
            }
        }

        return ExecutionResult.success(json);
    }
}
