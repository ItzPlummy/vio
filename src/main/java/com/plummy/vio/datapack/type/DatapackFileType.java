package com.plummy.vio.datapack.type;

import com.plummy.vio.datapack.DatapackValidation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

public sealed interface DatapackFileType permits CodecFileType, TagFileType, DataMapFileType, FunctionFileType {

    String getFolder();

    String getExtension();

    boolean isReloadable();

    DatapackValidation validate(MinecraftServer server, ResourceLocation id, String content);
}
