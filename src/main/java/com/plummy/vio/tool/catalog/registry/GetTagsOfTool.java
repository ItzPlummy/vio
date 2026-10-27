package com.plummy.vio.tool.catalog.registry;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.plummy.vio.access.AccessLevel;
import com.plummy.vio.tool.ServerTool;
import com.plummy.vio.tool.executor.ExecutionResult;
import com.plummy.vio.tool.value.argument.ArgumentDefinition;
import com.plummy.vio.tool.value.argument.ArgumentType;
import com.plummy.vio.tool.value.argument.Arguments;
import com.plummy.vio.tool.value.catalog.Identifier;
import com.plummy.vio.tool.value.catalog.Page;
import com.plummy.vio.tool.value.result.ResultValueJsonSerializer;
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

        schema.put("status", ArgumentDefinition.status());
        schema.put("registry", new ArgumentDefinition(ArgumentType.IDENTIFIER, "Registry id, e.g. minecraft:block, minecraft:item, minecraft:entity_type."));
        schema.put("id", new ArgumentDefinition(ArgumentType.IDENTIFIER, "Entry id, e.g. minecraft:oak_log."));
        schema.put("page", ArgumentDefinition.page(MAX_LIMIT, 50, "tags"));

        return schema;
    }

    @Override
    public AccessLevel getAccessLevel() {
        return AccessLevel.BASIC;
    }

    @Override
    public ExecutionResult execute(Context context, Arguments arguments) {
        Registry<?> registry = context.getRegistry(arguments.getIdentifier("registry"));

        return listTags(context, registry, arguments.getIdentifier("id"), arguments.getPage("page", MAX_LIMIT));
    }

    private static <T> ExecutionResult listTags(Context context, Registry<T> registry, Identifier id, Page page) {
        Holder.Reference<T> holder = context.getEntry(registry, id, "id");
        List<String> tags = holder.tags().map(key -> "#%s".formatted(key.location())).sorted().toList();

        JsonObject json = new JsonObject();

        json.addProperty("id", holder.key().location().toString());
        json.add("tags", page.apply(tags).stream().collect(JsonArray::new, JsonArray::add, JsonArray::addAll));
        json.add("page", page.withTotal(tags.size()).accept(ResultValueJsonSerializer.getInstance()));

        return ExecutionResult.success(json);
    }
}
