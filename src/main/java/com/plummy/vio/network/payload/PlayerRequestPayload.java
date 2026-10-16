package com.plummy.vio.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class PlayerRequestPayload implements CustomPacketPayload {

    private static final Type<PlayerRequestPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("vio", "player_request"));

    private static final StreamCodec<RegistryFriendlyByteBuf, PlayerRequestPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            PlayerRequestPayload::getRequest,
            PlayerRequestPayload::new
    );

    private final String request;

    public PlayerRequestPayload(String request) {
        this.request = request;
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return getType();
    }

    public String getRequest() {
        return request;
    }

    public static Type<PlayerRequestPayload> getType() {
        return TYPE;
    }

    public static StreamCodec<RegistryFriendlyByteBuf, PlayerRequestPayload> getStreamCodec() {
        return STREAM_CODEC;
    }
}
