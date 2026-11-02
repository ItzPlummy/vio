package com.plummy.vio.tool.catalog.datapack;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.plummy.vio.access.AccessLevel;
import com.plummy.vio.config.DatapackLimits;
import com.plummy.vio.datapack.DatapackManager;
import com.plummy.vio.datapack.DatapackPath;
import com.plummy.vio.datapack.type.DatapackFileType;
import com.plummy.vio.tool.ServerTool;
import com.plummy.vio.tool.executor.ExecutionResult;
import com.plummy.vio.tool.value.argument.ArgumentDefinition;
import com.plummy.vio.tool.value.argument.ArgumentType;
import com.plummy.vio.tool.value.argument.Arguments;
import com.plummy.vio.tool.value.catalog.Page;
import com.plummy.vio.tool.value.result.ResultValueJsonSerializer;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;

public class ListDatapackFilesTool implements ServerTool {

    @Override
    public String getName() {
        return "list_datapack_files";
    }

    @Override
    public String getDescription() {
        return "Files in Vio's data pack, sorted by path, each with its folder type and whether a reload applies it (reloadable) or it needs a server restart. A null type means the game ignores the file, usually because of a wrong folder. The pack is shared: every FULL player can use and change all of it.";
    }

    @Override
    public SequencedMap<String, ArgumentDefinition> getArgumentSchema() {
        SequencedMap<String, ArgumentDefinition> schema = new LinkedHashMap<>();

        schema.put("status", ArgumentDefinition.status());
        schema.put("namespace", new ArgumentDefinition(ArgumentType.STRING, "Only files in this namespace, e.g. 'vio'; empty for every namespace."));
        schema.put("folder", new ArgumentDefinition(ArgumentType.STRING, "Only files under this folder after the namespace, e.g. 'recipe' or 'tags/item'; empty for every folder."));
        schema.put("page", ArgumentDefinition.page(DatapackLimits.MAX_LIST_LIMIT, 100, "files"));

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
        String namespace = arguments.getString("namespace").strip();
        String folder = arguments.getString("folder").strip();
        Page page = arguments.getPage("page", DatapackLimits.MAX_LIST_LIMIT);
        DatapackManager manager = DatapackTools.getManager();

        String folderPrefix = folder.isEmpty() || folder.endsWith("/") ? folder : "%s/".formatted(folder);

        List<DatapackPath> paths = manager.getPaths().stream()
                .filter(path -> namespace.isEmpty() || path.getNamespace().equals(namespace))
                .filter(path -> path.getPath().startsWith(folderPrefix))
                .sorted(Comparator.comparing(DatapackPath::toString))
                .toList();

        JsonArray files = new JsonArray();

        for (DatapackPath path : page.apply(paths)) {
            DatapackFileType type = manager.getFileType(path);
            JsonObject file = new JsonObject();

            file.addProperty("path", path.toString());
            file.addProperty("type", type == null ? null : type.getFolder());
            file.addProperty("reloadable", type == null ? null : type.isReloadable());

            files.add(file);
        }

        JsonObject json = new JsonObject();

        json.add("files", files);
        json.add("page", page.withTotal(paths.size()).accept(ResultValueJsonSerializer.getInstance()));

        return ExecutionResult.success(json);
    }
}
