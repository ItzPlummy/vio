package com.plummy.vio.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class StartCapturePayload implements CustomPacketPayload {

    private static final StartCapturePayload INSTANCE = new StartCapturePayload();

    private static final Type<StartCapturePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("vio", "start_capture"));

    private static final StreamCodec<RegistryFriendlyByteBuf, StartCapturePayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    private StartCapturePayload() {
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return getType();
    }

    public static StartCapturePayload getInstance() {
        return INSTANCE;
    }

    public static Type<StartCapturePayload> getType() {
        return TYPE;
    }

    public static StreamCodec<RegistryFriendlyByteBuf, StartCapturePayload> getStreamCodec() {
        return STREAM_CODEC;
    }
}
