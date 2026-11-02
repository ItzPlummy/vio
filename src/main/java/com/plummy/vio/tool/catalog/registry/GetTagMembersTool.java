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
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.tags.TagKey;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.SequencedMap;

public class GetTagMembersTool implements ServerTool {

    private static final int MAX_LIMIT = 200;

    @Override
    public String getName() {
        return "get_tag_members";
    }

    @Override
    public String getDescription() {
        return "Members of a tag, nested tags flattened, sorted by id.";
    }

    @Override
    public SequencedMap<String, ArgumentDefinition> getArgumentSchema() {
        SequencedMap<String, ArgumentDefinition> schema = new LinkedHashMap<>();

        schema.put("status", ArgumentDefinition.status());
        schema.put("registry", new ArgumentDefinition(ArgumentType.IDENTIFIER, "Registry id, e.g. minecraft:block, minecraft:item, minecraft:entity_type."));
        schema.put("tag", new ArgumentDefinition(ArgumentType.IDENTIFIER, "Tag, e.g. #minecraft:logs."));
        schema.put("page", ArgumentDefinition.page(MAX_LIMIT, 50, "members"));

        return schema;
    }

    @Override
    public AccessLevel getAccessLevel() {
        return AccessLevel.BASIC;
    }

    @Override
    public ExecutionResult execute(Context context, Arguments arguments) {
        Registry<?> registry = context.getRegistry(arguments.getIdentifier("registry"));

        return listMembers(registry, arguments.getIdentifier("tag"), arguments.getPage("page", MAX_LIMIT));
    }

    private static <T> ExecutionResult listMembers(Registry<T> registry, Identifier tag, Page page) {
        TagKey<T> key = TagKey.create(registry.key(), tag.getId());
        Optional<HolderSet.Named<T>> holders = registry.getTag(key);

        if (holders.isEmpty()) {
            return ExecutionResult.invalidArguments("Unknown tag #%s".formatted(key.location()));
        }

        List<String> members = holders.get().stream()
                .map(holder -> holder.unwrapKey().map(resourceKey -> resourceKey.location().toString()).orElse("unknown"))
                .distinct()
                .sorted()
                .toList();

        JsonObject json = new JsonObject();

        json.addProperty("tag", "#%s".formatted(key.location()));
        json.add("members", page.apply(members).stream().collect(JsonArray::new, JsonArray::add, JsonArray::addAll));
        json.add("page", page.withTotal(members.size()).accept(ResultValueJsonSerializer.getInstance()));

        return ExecutionResult.success(json);
    }
}
