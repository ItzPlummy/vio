package com.plummy.vio.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class StopCapturePayload implements CustomPacketPayload {

    private static final StopCapturePayload INSTANCE = new StopCapturePayload();

    private static final Type<StopCapturePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("vio", "stop_capture"));

    private static final StreamCodec<RegistryFriendlyByteBuf, StopCapturePayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    private StopCapturePayload() {
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return getType();
    }

    public static StopCapturePayload getInstance() {
        return INSTANCE;
    }

    public static Type<StopCapturePayload> getType() {
        return TYPE;
    }

    public static StreamCodec<RegistryFriendlyByteBuf, StopCapturePayload> getStreamCodec() {
        return STREAM_CODEC;
    }
}
