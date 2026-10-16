package com.plummy.vio.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class ToolResponsePayload implements CustomPacketPayload {

    private static final Type<ToolResponsePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("vio", "tool_response"));

    private static final StreamCodec<RegistryFriendlyByteBuf, ToolResponsePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            ToolResponsePayload::getResponse,
            ToolResponsePayload::new
    );

    private final String response;

    public ToolResponsePayload(String response) {
        this.response = response;
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return getType();
    }

    public String getResponse() {
        return response;
    }

    public static Type<ToolResponsePayload> getType() {
        return TYPE;
    }

    public static StreamCodec<RegistryFriendlyByteBuf, ToolResponsePayload> getStreamCodec() {
        return STREAM_CODEC;
    }
}
