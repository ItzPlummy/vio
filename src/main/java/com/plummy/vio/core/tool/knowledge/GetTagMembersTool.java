package com.plummy.vio.core.tool.knowledge;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.plummy.vio.core.access.AccessLevel;
import com.plummy.vio.core.tool.ExecutionResult;
import com.plummy.vio.core.tool.ServerTool;
import com.plummy.vio.core.tool.argument.*;
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

        schema.put("registry", new ArgumentDefinition(ArgumentType.IDENTIFIER, "Registry id, e.g. minecraft:block, minecraft:item, minecraft:entity_type."));
        schema.put("tag", new ArgumentDefinition(ArgumentType.IDENTIFIER, "Tag, e.g. #minecraft:logs."));
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

        return listMembers(registry, arguments.getIdentifier("tag"), arguments.getPage(MAX_LIMIT));
    }

    // The leading # is optional here, since the argument can only be a tag.
    private static <T> ExecutionResult listMembers(Registry<T> registry, Identifier tag, Page page) {
        TagKey<T> key = TagKey.create(registry.key(), tag.getId());
        Optional<HolderSet.Named<T>> holders = registry.getTag(key);

        if (holders.isEmpty()) {
            return ExecutionResult.invalidArguments("Unknown tag \"#%s\".".formatted(key.location()));
        }

        List<String> members = holders.get().stream()
                .map(holder -> holder.unwrapKey().map(resourceKey -> resourceKey.location().toString()).orElse("unknown"))
                .distinct()
                .sorted()
                .toList();

        JsonObject json = new JsonObject();

        json.addProperty("tag", "#" + key.location());
        json.add("members", page.apply(members).stream().collect(JsonArray::new, JsonArray::add, JsonArray::addAll));
        page.write(json, members.size());

        return ExecutionResult.success(json);
    }
}
