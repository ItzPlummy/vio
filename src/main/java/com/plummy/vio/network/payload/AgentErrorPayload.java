package com.plummy.vio.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class AgentErrorPayload implements CustomPacketPayload {

    private static final Type<AgentErrorPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("vio", "agent_error"));

    private static final StreamCodec<RegistryFriendlyByteBuf, AgentErrorPayload> STREAM_CODEC = StreamCodec.composite(
            ComponentSerialization.STREAM_CODEC,
            AgentErrorPayload::getError,
            AgentErrorPayload::new
    );

    private final Component error;

    public AgentErrorPayload(Component error) {
        this.error = error;
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return getType();
    }

    public Component getError() {
        return error;
    }

    public static Type<AgentErrorPayload> getType() {
        return TYPE;
    }

    public static StreamCodec<RegistryFriendlyByteBuf, AgentErrorPayload> getStreamCodec() {
        return STREAM_CODEC;
    }
}
