package com.plummy.vio.core.tool.catalog.registry;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.plummy.vio.core.access.AccessLevel;
import com.plummy.vio.core.tool.executor.ExecutionResult;
import com.plummy.vio.core.tool.ServerTool;
import com.plummy.vio.core.tool.value.argument.ArgumentDefinition;
import com.plummy.vio.core.tool.value.argument.ArgumentType;
import com.plummy.vio.core.tool.value.argument.Arguments;
import com.plummy.vio.core.tool.value.catalog.Page;
import com.plummy.vio.core.tool.value.result.ResultValueJsonSerializer;
import net.minecraft.Util;
import net.minecraft.core.Registry;
import net.minecraft.locale.Language;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;

import java.util.*;

public class SearchRegistryTool implements ServerTool {

    private static final int MAX_LIMIT = 50;

    @Override
    public String getName() {
        return "search_registry";
    }

    @Override
    public String getDescription() {
        return "Search any registry (items, blocks, entity types, enchantments, biomes, structures, ...) for entries whose id or English name contains the query, case-insensitive, sorted by id. Use it to find the exact id before calling other tools. Names are always English.";
    }

    @Override
    public SequencedMap<String, ArgumentDefinition> getArgumentSchema() {
        SequencedMap<String, ArgumentDefinition> schema = new LinkedHashMap<>();

        schema.put("status", ArgumentDefinition.status());
        schema.put("registry", new ArgumentDefinition(ArgumentType.IDENTIFIER, "Registry id, e.g. minecraft:item, minecraft:block, minecraft:enchantment, minecraft:worldgen/biome."));
        schema.put("query", new ArgumentDefinition(ArgumentType.STRING, "Text the id or English name must contain, e.g. oak or diamond sword."));
        schema.put("page", ArgumentDefinition.page(MAX_LIMIT, 10, "matches"));

        return schema;
    }

    @Override
    public AccessLevel getAccessLevel() {
        return AccessLevel.BASIC;
    }

    @Override
    public ExecutionResult execute(Context context, Arguments arguments) {
        Registry<?> registry = context.getRegistry(arguments.getIdentifier("registry"));
        String query = arguments.getString("query");
        Page page = arguments.getPage("page", MAX_LIMIT);

        List<Match> matches = search(registry, query.strip().toLowerCase(Locale.ROOT));
        JsonArray results = new JsonArray();

        for (Match match : page.apply(matches)) {
            JsonObject result = new JsonObject();

            result.addProperty("id", match.id());
            result.addProperty("name", match.name());

            results.add(result);
        }

        JsonObject json = new JsonObject();

        json.addProperty("registry", registry.key().location().toString());
        json.add("results", results);
        json.add("page", page.withTotal(matches.size()).accept(ResultValueJsonSerializer.getInstance()));

        return ExecutionResult.success(json);
    }

    private static <T> List<Match> search(Registry<T> registry, String query) {
        List<Match> matches = new ArrayList<>();

        for (T value : registry) {
            String id = String.valueOf(registry.getKey(value));
            String name = switch (value) {
                case Item item -> item.getDescription().getString();
                case Block block -> block.getName().getString();
                case EntityType<?> entityType -> entityType.getDescription().getString();
                case Enchantment enchantment -> enchantment.description().getString();
                case MobEffect effect -> effect.getDisplayName().getString();
                default -> {
                    String registryPath = registry.key().location().getPath();
                    String key = Util.makeDescriptionId(registryPath.substring(registryPath.lastIndexOf('/') + 1), registry.getKey(value));

                    yield Language.getInstance().has(key) ? Language.getInstance().getOrDefault(key) : null;
                }
            };

            if (name != null && name.contains(".") && !name.contains(" ")) {
                name = null;
            }


            if (id.contains(query.replace(' ', '_')) || name != null && name.toLowerCase(Locale.ROOT).contains(query)) {
                matches.add(new Match(id, name));
            }
        }

        matches.sort(Comparator.comparing(Match::id));

        return matches;
    }

    private record Match(String id, String name) {
    }
}
