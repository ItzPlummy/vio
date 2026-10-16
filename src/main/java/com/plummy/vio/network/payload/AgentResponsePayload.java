package com.plummy.vio.network.payload;

import com.plummy.vio.network.Networking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

public class AgentResponsePayload implements CustomPacketPayload {

    private static final Type<AgentResponsePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("vio", "agent_response"));

    private static final StreamCodec<RegistryFriendlyByteBuf, AgentResponsePayload> STREAM_CODEC = StreamCodec.composite(
            Networking.TEXT_CODEC,
            AgentResponsePayload::getResponse,
            Networking.TIME_CODEC,
            AgentResponsePayload::getTime,
            AgentResponsePayload::new
    );

    private final String response;
    private final Instant time;

    public AgentResponsePayload(String response, Instant time) {
        this.response = response;
        this.time = time;
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return getType();
    }

    public String getResponse() {
        return response;
    }

    public Instant getTime() {
        return time;
    }

    public static Type<AgentResponsePayload> getType() {
        return TYPE;
    }

    public static StreamCodec<RegistryFriendlyByteBuf, AgentResponsePayload> getStreamCodec() {
        return STREAM_CODEC;
    }
}
