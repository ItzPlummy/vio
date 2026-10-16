package com.plummy.vio.client;

import com.plummy.vio.agent.SubmitResult;
import com.plummy.vio.network.payload.HistoryPayload;
import com.plummy.vio.network.payload.PlayerRequestPayload;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.resources.language.I18n;
import net.neoforged.neoforge.network.PacketDistributor;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Client-side copy of the conversation shown in the Vio screen. Only touched from the client thread.
public class ClientChatHistory {

    private static final int MAX_ENTRIES = 500;

    private static final ClientChatHistory INSTANCE = new ClientChatHistory();

    private final List<ChatEntry> entries = new ArrayList<>();

    private boolean busy;
    private ChatEntry pendingRequest;
    private String draft = "";
    // Shown in place of a status while Vio thinks before its first one; picked once per request.
    private String thinkingStatus = "";

    // Bumped on every change, so views can cache wrapped lines until it moves.
    private int revision;
    private long lastChangeMillis;

    public static ClientChatHistory getInstance() {
        return INSTANCE;
    }

    private ClientChatHistory() {
    }

    public boolean canSend() {
        return !busy && isAvailable();
    }

    public boolean isAvailable() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();

        return connection != null && connection.hasChannel(PlayerRequestPayload.getType());
    }

    public boolean send(String request) {
        String text = request.strip();

        if (text.isEmpty() || text.length() > PlayerRequestPayload.MAX_LENGTH || busy) {
            return false;
        }

        if (!isAvailable()) {
            add(new ChatEntry(ChatEntry.Kind.ERROR, I18n.get("vio.chat.error.not_installed"), Instant.now()));
            return false;
        }

        thinkingStatus = StatusPlaceholders.random();
        // Shown with the local time until the server answers with the time it stored.
        pendingRequest = new ChatEntry(ChatEntry.Kind.PLAYER, text, Instant.now());
        add(pendingRequest);
        busy = true;

        PacketDistributor.sendToServer(new PlayerRequestPayload(text));
        return true;
    }

    public void onSubmitResult(SubmitResult result, Instant time) {
        ChatEntry request = pendingRequest;
        pendingRequest = null;

        if (result == SubmitResult.ACCEPTED) {
            int index = entries.indexOf(request);

            // The server's time, so the request matches the stored one and the reply's thinking time adds up.
            if (index >= 0) {
                entries.set(index, new ChatEntry(ChatEntry.Kind.PLAYER, request.getText(), time));
                changed();
            }

            return;
        }

        // The server did not store a rejected request, so it should not stay in the history either.
        if (request != null) {
            entries.remove(request);
        }

        // BUSY means the server is still working on an earlier request, whose answer will clear the flag.
        busy = result == SubmitResult.BUSY;

        String key = switch (result) {
            case BUSY -> "vio.chat.error.busy";
            case DENIED -> "vio.chat.error.denied";
            default -> "vio.chat.error.unavailable";
        };

        add(new ChatEntry(ChatEntry.Kind.ERROR, I18n.get(key), Instant.now()));
    }

    public void onStatus(String status, Instant time) {
        busy = true;
        // A tool call without a status of its own gets a placeholder too.
        add(new ChatEntry(ChatEntry.Kind.STATUS, status.isBlank() ? StatusPlaceholders.random() : status, time));
    }

    public void onResponse(String response, Instant time) {
        busy = false;
        add(reply(response, time));
    }

    public void onError(String error) {
        busy = false;
        // Errors are not stored on the server, so they have no server time.
        add(new ChatEntry(ChatEntry.Kind.ERROR, error, Instant.now()));
    }

    public void onHistory(HistoryPayload payload) {
        entries.clear();

        for (HistoryPayload.Entry entry : payload.getEntries()) {
            ChatEntry chatEntry = ChatEntry.fromHistory(entry);
            entries.add(chatEntry.getKind() == ChatEntry.Kind.VIO ? reply(chatEntry.getText(), chatEntry.getTime()) : chatEntry);
        }

        busy = payload.isBusy();
        thinkingStatus = StatusPlaceholders.random();
        pendingRequest = null;
        changed();
    }

    public void clear() {
        entries.clear();
        busy = false;
        pendingRequest = null;
        changed();
    }

    public void reset() {
        clear();
        draft = "";
    }

    public List<ChatEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public ChatEntry getLastEntry() {
        return entries.isEmpty() ? null : entries.getLast();
    }

    public String getThinkingStatus() {
        return thinkingStatus;
    }

    public boolean isBusy() {
        return busy;
    }

    public String getDraft() {
        return draft;
    }

    public void setDraft(String draft) {
        this.draft = draft;
    }

    public int getRevision() {
        return revision;
    }

    public long getLastChangeMillis() {
        return lastChangeMillis;
    }

    // A Vio reply, timed from the player's message it answers: the last one before it.
    // Null when that message is not in the history, e.g. cut off by the history limit.
    private ChatEntry reply(String text, Instant time) {
        Duration thought = null;

        for (ChatEntry entry : entries.reversed()) {
            if (entry.getKind() == ChatEntry.Kind.PLAYER) {
                thought = Duration.between(entry.getTime(), time);
                break;
            }
        }

        return new ChatEntry(ChatEntry.Kind.VIO, text, time, thought);
    }

    private void add(ChatEntry entry) {
        entries.add(entry);

        if (entries.size() > MAX_ENTRIES) {
            entries.subList(0, entries.size() - MAX_ENTRIES).clear();
        }

        changed();
    }

    private void changed() {
        revision++;
        lastChangeMillis = Util.getMillis();
    }
}
