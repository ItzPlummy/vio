package com.plummy.vio.network.payload;

import com.plummy.vio.network.Networking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.List;

public class HistoryPayload implements CustomPacketPayload {

    public static final int MAX_ENTRIES = 200;

    private static final Type<HistoryPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("vio", "history"));

    private static final StreamCodec<RegistryFriendlyByteBuf, HistoryPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            HistoryPayload::isBusy,
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_ENTRIES)),
            HistoryPayload::getEntries,
            HistoryPayload::new
    );

    private final boolean busy;
    private final List<Entry> entries;

    public HistoryPayload(boolean busy, List<Entry> entries) {
        this.busy = busy;
        this.entries = List.copyOf(entries);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return getType();
    }

    public boolean isBusy() {
        return busy;
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public static Type<HistoryPayload> getType() {
        return TYPE;
    }

    public static StreamCodec<RegistryFriendlyByteBuf, HistoryPayload> getStreamCodec() {
        return STREAM_CODEC;
    }

    public enum Kind {

        PLAYER,
        AGENT
    }

    public static class Entry {

        private static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                NeoForgeStreamCodecs.enumCodec(Kind.class),
                Entry::getKind,
                Networking.TEXT_CODEC,
                Entry::getText,
                Networking.TIME_CODEC,
                Entry::getTime,
                ByteBufCodecs.BOOL,
                Entry::isVoice,
                Entry::new
        );

        private final Kind kind;
        private final String text;
        private final Instant time;
        private final boolean voice;

        public Entry(Kind kind, String text, Instant time, boolean voice) {
            this.kind = kind;
            this.text = text;
            this.time = time;
            this.voice = voice;
        }

        public Kind getKind() {
            return kind;
        }

        public String getText() {
            return text;
        }

        public Instant getTime() {
            return time;
        }

        // A transcribed request; always false for the agent's replies.
        public boolean isVoice() {
            return voice;
        }
    }
}
