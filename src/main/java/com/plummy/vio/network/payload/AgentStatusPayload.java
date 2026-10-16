package com.plummy.vio.network.payload;

import com.plummy.vio.network.Networking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

public class AgentStatusPayload implements CustomPacketPayload {

    private static final Type<AgentStatusPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("vio", "agent_status"));

    private static final StreamCodec<RegistryFriendlyByteBuf, AgentStatusPayload> STREAM_CODEC = StreamCodec.composite(
            Networking.TEXT_CODEC,
            AgentStatusPayload::getStatus,
            Networking.TIME_CODEC,
            AgentStatusPayload::getTime,
            AgentStatusPayload::new
    );

    private final String status;
    private final Instant time;

    public AgentStatusPayload(String status, Instant time) {
        this.status = status;
        this.time = time;
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return getType();
    }

    public String getStatus() {
        return status;
    }

    public Instant getTime() {
        return time;
    }

    public static Type<AgentStatusPayload> getType() {
        return TYPE;
    }

    public static StreamCodec<RegistryFriendlyByteBuf, AgentStatusPayload> getStreamCodec() {
        return STREAM_CODEC;
    }
}
