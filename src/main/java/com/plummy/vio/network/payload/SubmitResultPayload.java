package com.plummy.vio.network.payload;

import com.plummy.vio.agent.SubmitResult;
import com.plummy.vio.network.Networking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

public class SubmitResultPayload implements CustomPacketPayload {

    private static final Type<SubmitResultPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("vio", "submit_result"));

    private static final StreamCodec<RegistryFriendlyByteBuf, SubmitResultPayload> STREAM_CODEC = StreamCodec.composite(
            NeoForgeStreamCodecs.enumCodec(SubmitResult.class),
            SubmitResultPayload::getResult,
            Networking.TIME_CODEC,
            SubmitResultPayload::getTime,
            SubmitResultPayload::new
    );

    private final SubmitResult result;
    private final Instant time;

    public SubmitResultPayload(SubmitResult result, Instant time) {
        this.result = result;
        this.time = time;
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return getType();
    }

    public SubmitResult getResult() {
        return result;
    }

    // When the server received the request; the stored request's time if it was accepted.
    public Instant getTime() {
        return time;
    }

    public static Type<SubmitResultPayload> getType() {
        return TYPE;
    }

    public static StreamCodec<RegistryFriendlyByteBuf, SubmitResultPayload> getStreamCodec() {
        return STREAM_CODEC;
    }
}
