package com.plummy.vio.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class AgentResponsePayload implements CustomPacketPayload {

    private static final Type<AgentResponsePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("vio", "agent_response"));

    private static final StreamCodec<RegistryFriendlyByteBuf, AgentResponsePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            AgentResponsePayload::getResponse,
            AgentResponsePayload::new
    );

    private final String response;

    public AgentResponsePayload(String response) {
        this.response = response;
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return getType();
    }

    public String getResponse() {
        return response;
    }

    public static Type<AgentResponsePayload> getType() {
        return TYPE;
    }

    public static StreamCodec<RegistryFriendlyByteBuf, AgentResponsePayload> getStreamCodec() {
        return STREAM_CODEC;
    }
}
