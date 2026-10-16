package com.plummy.vio.client.conversation;

import com.plummy.vio.agent.SubmitResult;
import com.plummy.vio.client.StatusPlaceholders;
import com.plummy.vio.client.message.*;
import com.plummy.vio.network.payload.HistoryPayload;
import com.plummy.vio.network.payload.PlayerRequestPayload;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.resources.language.I18n;
import net.neoforged.neoforge.network.PacketDistributor;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class ClientConversation {

    private static final ClientConversation INSTANCE = new ClientConversation();

    private static final int MAX_MESSAGES = 500;

    private final List<ClientMessage> messages = new ArrayList<>();

    private boolean isBusy;
    private PlayerMessage pendingRequest;
    private String draft = "";
    private String thinkingStatus = "";

    private int revision;
    private long lastChangeMillis;

    public static ClientConversation getInstance() {
        return INSTANCE;
    }

    private ClientConversation() {
    }

    public List<ClientMessage> getMessages() {
        return messages.stream().toList();
    }

    public ClientMessage getLastMessage() {
        return messages.isEmpty() ? null : messages.getLast();
    }

    public boolean isBusy() {
        return isBusy;
    }

    public PlayerMessage getPendingRequest() {
        return pendingRequest;
    }

    public String getDraft() {
        return draft;
    }

    public void setDraft(String draft) {
        this.draft = draft;
    }

    public String getThinkingStatus() {
        return thinkingStatus;
    }

    public int getRevision() {
        return revision;
    }

    public long getLastChangeMillis() {
        return lastChangeMillis;
    }

    public boolean isAvailable() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        return connection != null && connection.hasChannel(PlayerRequestPayload.getType());
    }

    public boolean send(String request) {
        String text = request.strip();

        if (text.isEmpty() || text.length() > PlayerRequestPayload.MAX_LENGTH || isBusy()) {
            return false;
        }

        if (!isAvailable()) {
            addMessage(new ErrorMessage(I18n.get("vio.chat.error.not_installed"), Instant.now()));
            return false;
        }

        thinkingStatus = StatusPlaceholders.getRandom();
        pendingRequest = new PlayerMessage(text, Instant.now());
        addMessage(pendingRequest);
        isBusy = true;

        PacketDistributor.sendToServer(new PlayerRequestPayload(text));
        return true;
    }

    public void onSubmitResult(SubmitResult result, Instant time) {
        PlayerMessage request = pendingRequest;
        pendingRequest = null;

        if (result == SubmitResult.ACCEPTED) {
            int index = messages.indexOf(request);

            if (index >= 0) {
                messages.set(index, new PlayerMessage(request.getText(), time));
                onChange();
            }

            return;
        }

        if (request != null) {
            messages.remove(request);
        }

        isBusy = result == SubmitResult.BUSY;

        String key = switch (result) {
            case BUSY -> "vio.chat.error.busy";
            case DENIED -> "vio.chat.error.denied";
            default -> "vio.chat.error.unavailable";
        };

        addMessage(new ErrorMessage(I18n.get(key), Instant.now()));
    }

    public void onStatus(String status, Instant time) {
        isBusy = true;
        addMessage(new StatusMessage(status.isBlank() ? StatusPlaceholders.getRandom() : status, time));
    }

    public void onResponse(String response, Instant time) {
        isBusy = false;
        addMessage(getAgentMessage(response, time));
    }

    public void onError(String error) {
        isBusy = false;
        addMessage(new ErrorMessage(error, Instant.now()));
    }

    public void onHistory(HistoryPayload payload) {
        messages.clear();

        for (HistoryPayload.Entry entry : payload.getEntries()) {
            ClientMessage message = switch (entry.getKind()) {
                case PLAYER -> new PlayerMessage(entry.getText(), entry.getTime());
                case AGENT -> getAgentMessage(entry.getText(), entry.getTime());
            };

            messages.add(message);
        }

        isBusy = payload.isBusy();
        thinkingStatus = StatusPlaceholders.getRandom();
        pendingRequest = null;
        onChange();
    }

    public void clear() {
        messages.clear();
        isBusy = false;
        pendingRequest = null;
        onChange();
    }

    public void reset() {
        clear();
        draft = "";
    }

    private AgentMessage getAgentMessage(String text, Instant time) {
        Duration thoughtDuration = null;

        for (ClientMessage message : messages.reversed()) {
            if (message.getType() == ClientMessageType.PLAYER) {
                thoughtDuration = Duration.between(message.getTime(), time);
                break;
            }
        }

        return new AgentMessage(text, time, thoughtDuration);
    }

    private void addMessage(ClientMessage message) {
        messages.add(message);

        if (messages.size() > MAX_MESSAGES) {
            messages.removeFirst();
        }

        onChange();
    }

    private void onChange() {
        revision++;
        lastChangeMillis = Util.getMillis();
    }
}
