package com.plummy.vio.datapack.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.plummy.vio.datapack.DatapackException;
import com.plummy.vio.datapack.DatapackPath;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagFile;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DataPackRegistriesHooks;
import net.neoforged.neoforge.registries.RegistryManager;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DatapackFileTypes {

    private static final String GLOBAL_LOOT_MODIFIERS_NAMESPACE = "neoforge";
    private static final String GLOBAL_LOOT_MODIFIERS_PATH = "loot_modifiers/global_loot_modifiers.json";

    private static final Codec<GlobalLootModifiers> GLOBAL_LOOT_MODIFIERS_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("replace", false).forGetter(GlobalLootModifiers::replace),
            ResourceLocation.CODEC.listOf().fieldOf("entries").forGetter(GlobalLootModifiers::entries)
    ).apply(instance, GlobalLootModifiers::new));

    private static final Map<String, String> LEGACY_FOLDERS = Map.ofEntries(
            Map.entry("recipes", "recipe"),
            Map.entry("loot_tables", "loot_table"),
            Map.entry("advancements", "advancement"),
            Map.entry("predicates", "predicate"),
            Map.entry("item_modifiers", "item_modifier"),
            Map.entry("functions", "function"),
            Map.entry("structures", "structure"),
            Map.entry("tags/items", "tags/item"),
            Map.entry("tags/blocks", "tags/block"),
            Map.entry("tags/entity_types", "tags/entity_type"),
            Map.entry("tags/fluids", "tags/fluid"),
            Map.entry("tags/game_events", "tags/game_event"),
            Map.entry("tags/functions", "tags/function")
    );

    private final Map<String, DatapackFileType> types = new LinkedHashMap<>();
    private final DatapackFileType globalLootModifiers = new CodecFileType("loot_modifiers", GLOBAL_LOOT_MODIFIERS_CODEC, true);

    public DatapackFileTypes(MinecraftServer server) {
        add(new CodecFileType("recipe", Recipe.CONDITIONAL_CODEC, true));
        add(new CodecFileType("advancement", Advancement.CONDITIONAL_CODEC, true));
        LootDataType.values().forEach(type -> add(new CodecFileType(Registries.elementsDirPath(type.registryKey()), type.conditionalCodec(), true)));
        add(new FunctionFileType());
        add(new CodecFileType("tags/function", TagFile.CODEC, true));
        add(new CodecFileType("loot_modifiers", IGlobalLootModifier.CONDITIONAL_CODEC, true));
        DataPackRegistriesHooks.getDataPackRegistriesWithDimensions().forEach(data -> add(new CodecFileType(Registries.elementsDirPath(data.key()), data.elementCodec(), false)));
        server.registryAccess().registries().forEach(entry -> add(new TagFileType(entry.key())));
        RegistryManager.getDataMaps().keySet().forEach(key -> add(new DataMapFileType(key)));
    }

    public DatapackTarget resolve(DatapackPath path) {
        String file = path.getPath();

        if (path.getNamespace().equals(GLOBAL_LOOT_MODIFIERS_NAMESPACE) && file.equals(GLOBAL_LOOT_MODIFIERS_PATH)) {
            return new DatapackTarget(globalLootModifiers, ResourceLocation.fromNamespaceAndPath(path.getNamespace(), "global_loot_modifiers"));
        }

        for (Map.Entry<String, String> entry : LEGACY_FOLDERS.entrySet()) {
            if (file.startsWith("%s/".formatted(entry.getKey()))) {
                throw new DatapackException("'%s' is the old plural folder name, which 1.21 silently ignores: use '%s' instead".formatted(entry.getKey(), entry.getValue()));
            }
        }

        if (file.startsWith("structure/")) {
            throw new DatapackException("Structure templates are binary NBT files, which Vio can't write");
        }

        DatapackFileType type = null;

        for (DatapackFileType candidate : types.values()) {
            if (file.startsWith("%s/".formatted(candidate.getFolder())) && (type == null || candidate.getFolder().length() > type.getFolder().length())) {
                type = candidate;
            }
        }

        if (type == null) {
            throw new DatapackException("'%s' isn't in a data pack folder this server loads, so the game would ignore it. The path after the namespace starts with the folder, e.g. recipe/, loot_table/, advancement/, function/, tags/item/ or enchantment/".formatted(path));
        }

        String name = file.substring(type.getFolder().length() + 1);
        String extension = ".%s".formatted(type.getExtension());

        if (!name.endsWith(extension) || name.length() == extension.length()) {
            throw new DatapackException("Files in %s/ must end in %s".formatted(type.getFolder(), extension));
        }

        return new DatapackTarget(type, ResourceLocation.fromNamespaceAndPath(path.getNamespace(), name.substring(0, name.length() - extension.length())));
    }

    public DatapackFileType find(DatapackPath path) {
        try {
            return resolve(path).type();
        } catch (DatapackException e) {
            return null;
        }
    }

    private void add(DatapackFileType type) {
        types.putIfAbsent(type.getFolder(), type);
    }

    private record GlobalLootModifiers(boolean replace, List<ResourceLocation> entries) {
    }
}
