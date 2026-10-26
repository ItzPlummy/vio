package com.plummy.vio.client.conversation;

import com.plummy.vio.agent.SubmitResult;
import com.plummy.vio.client.StatusPlaceholders;
import com.plummy.vio.client.message.*;
import com.plummy.vio.config.AgentLimits;
import com.plummy.vio.network.payload.*;
import com.plummy.vio.voice.VoiceAvailability;
import com.plummy.vio.voice.VoiceState;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.resources.language.I18n;
import net.neoforged.neoforge.network.PacketDistributor;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ClientConversation {

    private static final ClientConversation INSTANCE = new ClientConversation();

    private static final int MAX_MESSAGES = 500;

    private final List<ClientMessage> messages = new ArrayList<>();

    private boolean isBusy;
    private PlayerMessage pendingRequest;
    private String draft = "";
    private String thinkingStatus = "";
    private VoiceAvailability voiceAvailability = VoiceAvailability.UNSUPPORTED;
    private VoiceState voiceState = VoiceState.IDLE;

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

    public int getRevision() {
        return revision;
    }

    public long getLastChangeMillis() {
        return lastChangeMillis;
    }

    // A server without Vio never says, so it counts as one without voice.
    public VoiceAvailability getVoiceAvailability() {
        return isAvailable() ? voiceAvailability : VoiceAvailability.UNSUPPORTED;
    }

    public String getVoiceUnavailableReason() {
        return I18n.get("vio.voice.unavailable.%s".formatted(getVoiceAvailability().getValue()));
    }

    public VoiceState getVoiceState() {
        return voiceState;
    }

    // What voice is doing, as the capture's bubble and the status window say it; null when there's nothing to say.
    public String getVoiceStatus() {
        return switch (voiceState) {
            case LISTENING -> I18n.get("vio.voice.listening");
            case NO_AUDIO -> I18n.get("vio.voice.no_audio");
            case TRANSCRIBING -> I18n.get("vio.voice.transcribing");
            case IDLE, SPEAKING -> null;
        };
    }

    // The agent's latest status, or a placeholder until it sends one; null when it isn't working.
    public String getWorkingStatus() {
        if (!isBusy) {
            return null;
        }

        ClientMessage last = getLastMessage();

        return last != null && last.getType() == ClientMessageType.STATUS ? last.getText() : thinkingStatus;
    }

    public boolean isCapturing() {
        return voiceState == VoiceState.LISTENING || voiceState == VoiceState.NO_AUDIO;
    }

    // The icon and the status window show the outcome once the server starts listening, or why it can't.
    public void startCapture() {
        if (getVoiceAvailability() != VoiceAvailability.AVAILABLE) {
            addError(getVoiceUnavailableReason());
            return;
        }

        PacketDistributor.sendToServer(StartCapturePayload.getInstance());
    }

    public void stopCapture() {
        PacketDistributor.sendToServer(StopCapturePayload.getInstance());
    }

    public void cancelCapture() {
        PacketDistributor.sendToServer(CancelCapturePayload.getInstance());
    }

    public boolean isAvailable() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        return connection != null && connection.hasChannel(PlayerRequestPayload.getType());
    }

    public boolean send(String request) {
        String text = request.strip();

        // Not while talking: the live bubble is the pending request until the capture is sent.
        if (text.isEmpty() || text.length() > AgentLimits.MAX_REQUEST_LENGTH || isBusy() || isCapturing() || voiceState == VoiceState.TRANSCRIBING) {
            return false;
        }

        if (!isAvailable()) {
            addError(I18n.get("vio.chat.error.not_installed"));
            return false;
        }

        thinkingStatus = StatusPlaceholders.getRandom();
        pendingRequest = new PlayerMessage(text, Instant.now(), false);
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
                messages.set(index, new PlayerMessage(request.getText(), time, request.isVoice()));
            }

            // A spoken request only becomes one now, once its capture has ended.
            if (request != null && request.isVoice()) {
                thinkingStatus = StatusPlaceholders.getRandom();
                isBusy = true;
            }

            onChange();
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

        addError(I18n.get(key));
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
        addError(error);
    }

    public void onHistory(HistoryPayload payload) {
        messages.clear();

        for (HistoryPayload.Entry entry : payload.getEntries()) {
            ClientMessage message = switch (entry.getKind()) {
                case PLAYER -> new PlayerMessage(entry.getText(), entry.getTime(), entry.isVoice());
                case AGENT -> getAgentMessage(entry.getText(), entry.getTime());
            };

            messages.add(message);
        }

        isBusy = payload.isBusy();
        thinkingStatus = StatusPlaceholders.getRandom();
        pendingRequest = null;
        onChange();
    }

    public void onVoiceAvailability(VoiceAvailability availability) {
        voiceAvailability = availability;
        onChange();
    }

    public void onVoiceState(VoiceState state) {
        VoiceState previous = voiceState;
        voiceState = state;

        switch (state) {
            case LISTENING, NO_AUDIO -> {
                if (pendingRequest == null) {
                    pendingRequest = new PlayerMessage("", Instant.now(), true);
                    addMessage(pendingRequest);
                }
            }
            case IDLE -> {
                // A sent capture's bubble was already settled by its result, so this one was cancelled or empty.
                if (pendingRequest != null && pendingRequest.isVoice()) {
                    messages.remove(pendingRequest);
                    pendingRequest = null;
                }
            }
            case TRANSCRIBING, SPEAKING -> {
            }
        }

        onChange();
    }

    public void onVoiceTranscript(String transcript) {
        int index = messages.indexOf(pendingRequest);

        if (index < 0 || !pendingRequest.isVoice()) {
            return;
        }

        pendingRequest = new PlayerMessage(transcript, pendingRequest.getTime(), true);
        messages.set(index, pendingRequest);
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
        voiceAvailability = VoiceAvailability.UNSUPPORTED;
        voiceState = VoiceState.IDLE;
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

    private void addError(String error) {
        addMessage(new ErrorMessage(error, Instant.now()));
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
