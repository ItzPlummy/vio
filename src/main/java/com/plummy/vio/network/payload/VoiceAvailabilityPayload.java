package com.plummy.vio.network.payload;

import com.plummy.vio.voice.VoiceAvailability;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import org.jetbrains.annotations.NotNull;

public class VoiceAvailabilityPayload implements CustomPacketPayload {

    private static final Type<VoiceAvailabilityPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("vio", "voice_availability"));

    private static final StreamCodec<RegistryFriendlyByteBuf, VoiceAvailabilityPayload> STREAM_CODEC = StreamCodec.composite(
            NeoForgeStreamCodecs.enumCodec(VoiceAvailability.class),
            VoiceAvailabilityPayload::getAvailability,
            VoiceAvailabilityPayload::new
    );

    private final VoiceAvailability availability;

    public VoiceAvailabilityPayload(VoiceAvailability availability) {
        this.availability = availability;
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return getType();
    }

    public VoiceAvailability getAvailability() {
        return availability;
    }

    public static Type<VoiceAvailabilityPayload> getType() {
        return TYPE;
    }

    public static StreamCodec<RegistryFriendlyByteBuf, VoiceAvailabilityPayload> getStreamCodec() {
        return STREAM_CODEC;
    }
}
