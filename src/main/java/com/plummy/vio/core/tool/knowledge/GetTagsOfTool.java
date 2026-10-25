package com.plummy.vio.core.tool.knowledge;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.plummy.vio.core.access.AccessLevel;
import com.plummy.vio.core.tool.ExecutionResult;
import com.plummy.vio.core.tool.ServerTool;
import com.plummy.vio.core.tool.argument.*;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;

public class GetTagsOfTool implements ServerTool {

    private static final int MAX_LIMIT = 200;

    @Override
    public String getName() {
        return "get_tags_of";
    }

    @Override
    public String getDescription() {
        return "The tags a registry entry belongs to, sorted.";
    }

    @Override
    public SequencedMap<String, ArgumentDefinition> getArgumentSchema() {
        SequencedMap<String, ArgumentDefinition> schema = new LinkedHashMap<>();

        schema.put("registry", new ArgumentDefinition(ArgumentType.IDENTIFIER, "Registry id, e.g. minecraft:block, minecraft:item, minecraft:entity_type."));
        schema.put("id", new ArgumentDefinition(ArgumentType.IDENTIFIER, "Entry id, e.g. minecraft:oak_log."));
        schema.put("limit", new ArgumentDefinition(ArgumentType.INTEGER, "Page size, 1 to 200; 50 is a good default."));
        schema.put("offset", new ArgumentDefinition(ArgumentType.INTEGER, "Entries to skip; 0 for the first page."));

        return schema;
    }

    @Override
    public AccessLevel getAccessLevel() {
        return AccessLevel.BASIC;
    }

    @Override
    public ExecutionResult execute(Context context, Arguments arguments) {
        Registry<?> registry = context.getRegistry(arguments.getIdentifier("registry"));

        return listTags(context, registry, arguments.getIdentifier("id"), arguments.getPage(MAX_LIMIT));
    }

    private static <T> ExecutionResult listTags(Context context, Registry<T> registry, Identifier id, Page page) {
        Holder.Reference<T> holder = context.getEntry(registry, id, "id");
        List<String> tags = holder.tags().map(key -> "#" + key.location()).sorted().toList();

        JsonObject json = new JsonObject();

        json.addProperty("id", holder.key().location().toString());
        json.add("tags", page.apply(tags).stream().collect(JsonArray::new, JsonArray::add, JsonArray::addAll));
        page.write(json, tags.size());

        return ExecutionResult.success(json);
    }
}
