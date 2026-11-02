package com.plummy.vio.datapack.type;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.plummy.vio.datapack.DatapackValidation;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;

import java.util.Optional;

public final class CodecValidator {

    private CodecValidator() {
    }

    public static DatapackValidation validate(MinecraftServer server, Codec<?> codec, String content) {
        JsonElement json;

        try {
            json = JsonParser.parseString(content);
        } catch (JsonParseException e) {
            return DatapackValidation.error("Invalid JSON: %s".formatted(e.getMessage()));
        }

        RecordingRegistryLookup lookup = new RecordingRegistryLookup(server.reloadableRegistries().get());
        Optional<? extends DataResult.Error<?>> error;

        try {
            error = codec.parse(RegistryOps.create(JsonOps.INSTANCE, lookup), json).error();
        } catch (RuntimeException e) {
            return DatapackValidation.error("Couldn't parse the file: %s".formatted(e));
        }

        if (error.isPresent()) {
            return DatapackValidation.error(error.get().message());
        }

        return DatapackValidation.warnings(lookup.getMissing());
    }
}
