package com.plummy.vio.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class ClearHistoryPayload implements CustomPacketPayload {

    private static final ClearHistoryPayload INSTANCE = new ClearHistoryPayload();

    private static final Type<ClearHistoryPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("vio", "clear_history"));

    private static final StreamCodec<RegistryFriendlyByteBuf, ClearHistoryPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    private ClearHistoryPayload() {
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return getType();
    }

    public static ClearHistoryPayload getInstance() {
        return INSTANCE;
    }

    public static Type<ClearHistoryPayload> getType() {
        return TYPE;
    }

    public static StreamCodec<RegistryFriendlyByteBuf, ClearHistoryPayload> getStreamCodec() {
        return STREAM_CODEC;
    }
}
