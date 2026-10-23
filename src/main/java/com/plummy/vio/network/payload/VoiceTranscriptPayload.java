package com.plummy.vio.network.payload;

import com.plummy.vio.network.Networking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

// The whole transcript so far, sent each time a phrase is transcribed; it replaces the previous one.
public class VoiceTranscriptPayload implements CustomPacketPayload {

    private static final Type<VoiceTranscriptPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("vio", "voice_transcript"));

    private static final StreamCodec<RegistryFriendlyByteBuf, VoiceTranscriptPayload> STREAM_CODEC = StreamCodec.composite(
            Networking.TEXT_CODEC,
            VoiceTranscriptPayload::getTranscript,
            VoiceTranscriptPayload::new
    );

    private final String transcript;

    public VoiceTranscriptPayload(String transcript) {
        this.transcript = transcript;
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return getType();
    }

    public String getTranscript() {
        return transcript;
    }

    public static Type<VoiceTranscriptPayload> getType() {
        return TYPE;
    }

    public static StreamCodec<RegistryFriendlyByteBuf, VoiceTranscriptPayload> getStreamCodec() {
        return STREAM_CODEC;
    }
}
