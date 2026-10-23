package com.plummy.vio.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class CancelCapturePayload implements CustomPacketPayload {

    private static final CancelCapturePayload INSTANCE = new CancelCapturePayload();

    private static final Type<CancelCapturePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("vio", "cancel_capture"));

    private static final StreamCodec<RegistryFriendlyByteBuf, CancelCapturePayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    private CancelCapturePayload() {
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return getType();
    }

    public static CancelCapturePayload getInstance() {
        return INSTANCE;
    }

    public static Type<CancelCapturePayload> getType() {
        return TYPE;
    }

    public static StreamCodec<RegistryFriendlyByteBuf, CancelCapturePayload> getStreamCodec() {
        return STREAM_CODEC;
    }
}
