package com.plummy.vio.client;

import com.plummy.vio.network.payload.HistoryPayload;

import java.time.Duration;
import java.time.Instant;

public class ChatEntry {

    private final Kind kind;
    private final String text;
    // In UTC; shown in the player's local time zone.
    private final Instant time;
    // Vio's replies only: how long Vio took since the player's message. Null for everything else.
    private final Duration thought;

    public ChatEntry(Kind kind, String text, Instant time) {
        this(kind, text, time, null);
    }

    public ChatEntry(Kind kind, String text, Instant time, Duration thought) {
        this.kind = kind;
        this.text = text;
        this.time = time;
        this.thought = thought;
    }

    public static ChatEntry fromHistory(HistoryPayload.Entry entry) {
        Kind kind = switch (entry.getKind()) {
            case PLAYER -> Kind.PLAYER;
            case VIO -> Kind.VIO;
            case STATUS -> Kind.STATUS;
        };

        return new ChatEntry(kind, entry.getText(), entry.getTime());
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

    public Duration getThought() {
        return thought;
    }

    public enum Kind {

        PLAYER,
        VIO,
        STATUS,
        ERROR
    }
}
