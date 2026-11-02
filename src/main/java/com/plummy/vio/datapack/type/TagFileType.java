package com.plummy.vio.datapack.type;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.plummy.vio.datapack.DatapackValidation;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagEntry;
import net.minecraft.tags.TagFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class TagFileType implements DatapackFileType {

    private final ResourceKey<? extends Registry<?>> registryKey;

    public TagFileType(ResourceKey<? extends Registry<?>> registryKey) {
        this.registryKey = registryKey;
    }

    @Override
    public String getFolder() {
        return Registries.tagsDirPath(registryKey);
    }

    @Override
    public String getExtension() {
        return "json";
    }

    @Override
    public boolean isReloadable() {
        return true;
    }

    @Override
    public DatapackValidation validate(MinecraftServer server, ResourceLocation id, String content) {
        DatapackValidation validation = CodecValidator.validate(server, TagFile.CODEC, content);

        if (!validation.isValid()) {
            return validation;
        }

        TagFile file = TagFile.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(content)).getOrThrow();
        Optional<? extends Registry<?>> registry = server.reloadableRegistries().get().registry(registryKey);
        List<String> warnings = new ArrayList<>(validation.warnings());

        if (registry.isEmpty()) {
            return DatapackValidation.error("Registry %s isn't loaded on this server".formatted(registryKey.location()));
        }

        for (TagEntry entry : file.entries()) {
            if (!entry.isRequired()) {
                continue;
            }

            if (entry.isTag() && registry.get().getTagNames().noneMatch(tag -> tag.location().equals(entry.getId()))) {
                warnings.add("Unknown %s tag '#%s': unless it's added before the reload, the whole tag fails to load".formatted(registryKey.location(), entry.getId()));
            } else if (!entry.isTag() && !registry.get().containsKey(entry.getId())) {
                warnings.add("Unknown %s '%s': unless it's added before the reload, the whole tag fails to load; mark it {\"id\": ..., \"required\": false} if it may be missing".formatted(registryKey.location(), entry.getId()));
            }
        }

        return new DatapackValidation(List.of(), warnings);
    }
}
