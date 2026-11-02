package com.plummy.vio.datapack.type;

import com.mojang.serialization.Codec;
import com.plummy.vio.datapack.DatapackValidation;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.registries.DataMapLoader;
import net.neoforged.neoforge.registries.RegistryManager;
import net.neoforged.neoforge.registries.datamaps.DataMapFile;
import net.neoforged.neoforge.registries.datamaps.DataMapType;

public final class DataMapFileType implements DatapackFileType {

    private final ResourceKey<? extends Registry<?>> registryKey;

    public DataMapFileType(ResourceKey<? extends Registry<?>> registryKey) {
        this.registryKey = registryKey;
    }

    @Override
    public String getFolder() {
        return "%s/%s".formatted(DataMapLoader.PATH, DataMapLoader.getFolderLocation(registryKey.location()));
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
        Codec<?> codec = getCodec(id);

        if (codec == null) {
            return DatapackValidation.error("There's no data map '%s' for %s, so the file would be ignored. The file's name is the data map's id, e.g. neoforge/data_maps/item/furnace_fuels.json".formatted(id, registryKey.location()));
        }

        return CodecValidator.validate(server, codec, content);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Codec<?> getCodec(ResourceLocation id) {
        DataMapType type = RegistryManager.getDataMap((ResourceKey) registryKey, id);

        return type == null ? null : DataMapFile.codec((ResourceKey) registryKey, type);
    }
}
