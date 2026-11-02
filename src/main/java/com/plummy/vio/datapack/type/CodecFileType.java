package com.plummy.vio.datapack.type;

import com.mojang.serialization.Codec;
import com.plummy.vio.datapack.DatapackValidation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

public final class CodecFileType implements DatapackFileType {

    private final String folder;
    private final Codec<?> codec;
    private final boolean isReloadable;

    public CodecFileType(String folder, Codec<?> codec, boolean isReloadable) {
        this.folder = folder;
        this.codec = codec;
        this.isReloadable = isReloadable;
    }

    @Override
    public String getFolder() {
        return folder;
    }

    @Override
    public String getExtension() {
        return "json";
    }

    @Override
    public boolean isReloadable() {
        return isReloadable;
    }

    @Override
    public DatapackValidation validate(MinecraftServer server, ResourceLocation id, String content) {
        return CodecValidator.validate(server, codec, content);
    }
}
