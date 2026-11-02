package com.plummy.vio.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class ResetConversationPayload implements CustomPacketPayload {

    private static final ResetConversationPayload INSTANCE = new ResetConversationPayload();

    private static final Type<ResetConversationPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("vio", "reset_conversation"));

    private static final StreamCodec<RegistryFriendlyByteBuf, ResetConversationPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    private ResetConversationPayload() {
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return getType();
    }

    public static ResetConversationPayload getInstance() {
        return INSTANCE;
    }

    public static Type<ResetConversationPayload> getType() {
        return TYPE;
    }

    public static StreamCodec<RegistryFriendlyByteBuf, ResetConversationPayload> getStreamCodec() {
        return STREAM_CODEC;
    }
}
