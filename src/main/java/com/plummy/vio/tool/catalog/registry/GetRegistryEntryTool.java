package com.plummy.vio.tool.catalog.registry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.plummy.vio.access.AccessLevel;
import com.plummy.vio.tool.ServerTool;
import com.plummy.vio.tool.executor.ExecutionResult;
import com.plummy.vio.tool.value.argument.ArgumentDefinition;
import com.plummy.vio.tool.value.argument.ArgumentType;
import com.plummy.vio.tool.value.argument.Arguments;
import com.plummy.vio.tool.value.catalog.Identifier;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.registries.DataPackRegistriesHooks;

import java.util.*;

public class GetRegistryEntryTool implements ServerTool {

    @Override
    public String getName() {
        return "get_registry_entry";
    }

    @Override
    public String getDescription() {
        return "Full definition of a data-driven registry entry (enchantment, biome, structure, damage type, trim material, painting variant, dimension type, ...) as JSON encoded with its codec. Large definitions keep the top-level keys that fit and list the rest in omitted_keys. Static registries (blocks, items, entity types) have no definition; use get_item_info or get_block_type_info.";
    }

    @Override
    public SequencedMap<String, ArgumentDefinition> getArgumentSchema() {
        SequencedMap<String, ArgumentDefinition> schema = new LinkedHashMap<>();

        schema.put("status", ArgumentDefinition.status());
        schema.put("registry", new ArgumentDefinition(ArgumentType.IDENTIFIER, "Registry id, e.g. minecraft:enchantment, minecraft:worldgen/biome."));
        schema.put("id", new ArgumentDefinition(ArgumentType.IDENTIFIER, "Entry id."));

        return schema;
    }

    @Override
    public AccessLevel getAccessLevel() {
        return AccessLevel.BASIC;
    }

    @Override
    public ExecutionResult execute(Context context, Arguments arguments) {
        Registry<?> registry = context.getRegistry(arguments.getIdentifier("registry"));

        if (BuiltInRegistries.REGISTRY.containsKey(registry.key().location())) {
            return ExecutionResult.invalidArguments("%s is a static registry with no data-driven definitions; use get_item_info, get_block_type_info or search_registry instead".formatted(registry.key().location()));
        }

        return describe(context, registry, arguments.getIdentifier("id"));
    }

    private static <T> ExecutionResult describe(Context context, Registry<T> registry, Identifier id) {
        MinecraftServer server = context.getServer();
        Holder.Reference<T> holder = context.getEntry(registry, id, "entry");
        Codec<T> codec = findCodec(registry);

        if (codec == null) {
            return ExecutionResult.failure("No codec is known for %s, so its entries can't be shown".formatted(registry.key().location()));
        }

        JsonElement definition = codec.encodeStart(RegistryOps.create(JsonOps.INSTANCE, server.registryAccess()), holder.value())
                .resultOrPartial(error -> {
                })
                .orElse(null);

        if (definition == null) {
            return ExecutionResult.failure("%s couldn't be encoded".formatted(holder.key().location()));
        }

        JsonObject json = new JsonObject();

        json.addProperty("registry", registry.key().location().toString());
        json.addProperty("id", holder.key().location().toString());

        int maxChars = 32768;

        if (definition.toString().length() <= maxChars || !definition.isJsonObject()) {
            json.add("definition", definition);
            json.addProperty("truncated", false);
            return ExecutionResult.success(json);
        }

        JsonObject kept = new JsonObject();
        List<String> omitted = new ArrayList<>();
        int length = 2;

        for (Map.Entry<String, JsonElement> entry : definition.getAsJsonObject().entrySet()) {
            int entryLength = entry.getKey().length() + entry.getValue().toString().length() + 4;

            if (length + entryLength <= maxChars) {
                kept.add(entry.getKey(), entry.getValue());
                length += entryLength;
            } else {
                omitted.add(entry.getKey());
            }
        }

        json.add("definition", kept);
        json.add("omitted_keys", omitted.stream().collect(JsonArray::new, JsonArray::add, JsonArray::addAll));
        json.addProperty("truncated", true);

        return ExecutionResult.success(json);
    }

    @SuppressWarnings("unchecked")
    private static <T> Codec<T> findCodec(Registry<T> registry) {
        return DataPackRegistriesHooks.getDataPackRegistriesWithDimensions()
                .filter(data -> data.key().equals(registry.key()))
                .map(data -> (Codec<T>) data.elementCodec())
                .findFirst()
                .orElse(null);
    }
}
