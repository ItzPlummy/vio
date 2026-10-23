package com.plummy.vio.network.payload;

import com.plummy.vio.voice.VoiceState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import org.jetbrains.annotations.NotNull;

public class VoiceStatePayload implements CustomPacketPayload {

    private static final Type<VoiceStatePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("vio", "voice_state"));

    private static final StreamCodec<RegistryFriendlyByteBuf, VoiceStatePayload> STREAM_CODEC = StreamCodec.composite(
            NeoForgeStreamCodecs.enumCodec(VoiceState.class),
            VoiceStatePayload::getState,
            VoiceStatePayload::new
    );

    private final VoiceState state;

    public VoiceStatePayload(VoiceState state) {
        this.state = state;
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return getType();
    }

    public VoiceState getState() {
        return state;
    }

    public static Type<VoiceStatePayload> getType() {
        return TYPE;
    }

    public static StreamCodec<RegistryFriendlyByteBuf, VoiceStatePayload> getStreamCodec() {
        return STREAM_CODEC;
    }
}
