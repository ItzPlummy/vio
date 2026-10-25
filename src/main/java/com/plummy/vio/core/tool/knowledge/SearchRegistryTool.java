package com.plummy.vio.core.tool.knowledge;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.plummy.vio.core.access.AccessLevel;
import com.plummy.vio.core.tool.ExecutionResult;
import com.plummy.vio.core.tool.ServerTool;
import com.plummy.vio.core.tool.argument.ArgumentDefinition;
import com.plummy.vio.core.tool.argument.ArgumentType;
import com.plummy.vio.core.tool.argument.Arguments;
import com.plummy.vio.core.tool.argument.Page;
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

        schema.put("registry", new ArgumentDefinition(ArgumentType.IDENTIFIER, "Registry id, e.g. minecraft:item, minecraft:block, minecraft:enchantment, minecraft:worldgen/biome."));
        schema.put("query", new ArgumentDefinition(ArgumentType.STRING, "Text the id or English name must contain, e.g. oak or diamond sword."));
        schema.put("limit", new ArgumentDefinition(ArgumentType.INTEGER, "Page size, 1 to 50; 10 is a good default."));
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
        String query = arguments.getString("query");
        Page page = arguments.getPage(MAX_LIMIT);

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
        page.write(json, matches.size());

        return ExecutionResult.success(json);
    }

    // Spaces match underscores, so "diamond sword" finds minecraft:diamond_sword.
    private static <T> List<Match> search(Registry<T> registry, String query) {
        List<Match> matches = new ArrayList<>();

        for (T value : registry) {
            String id = String.valueOf(registry.getKey(value));
            // English display name, or null. A dedicated server only has en_us, so names are English regardless of the client.
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

            // Untranslated entries echo their key back, which is not a name.
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
