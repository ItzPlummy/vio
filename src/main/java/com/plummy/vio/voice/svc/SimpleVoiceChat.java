package com.plummy.vio.voice.svc;

import com.plummy.vio.voice.VoiceAvailability;
import com.plummy.vio.voice.VoiceChat;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.AudioPlayer;
import de.maxhenkel.voicechat.api.audiochannel.StaticAudioChannel;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class SimpleVoiceChat implements VoiceChat {

    private final VoicechatServerApi api;

    private final Map<UUID, Listener> listeners = new ConcurrentHashMap<>();
    private final Map<UUID, AudioPlayer> players = new ConcurrentHashMap<>();

    public SimpleVoiceChat(VoicechatServerApi api) {
        this.api = api;
    }

    public VoicechatServerApi getApi() {
        return api;
    }

    @Override
    public VoiceAvailability getAvailability(UUID playerId) {
        VoicechatConnection connection = getApi().getConnectionOf(playerId);

        // No connection yet right after joining, before the client's voice chat has connected.
        if (connection == null) {
            return VoiceAvailability.DISCONNECTED;
        }

        if (!connection.isInstalled()) {
            return VoiceAvailability.NOT_INSTALLED;
        }

        if (!connection.isConnected()) {
            return VoiceAvailability.DISCONNECTED;
        }

        if (connection.isDisabled()) {
            return VoiceAvailability.DISABLED;
        }

        return VoiceAvailability.AVAILABLE;
    }

    @Override
    public void startListening(UUID playerId, Consumer<short[]> frames) {
        Listener previous = listeners.put(playerId, new Listener(getApi().createDecoder(), frames));

        if (previous != null) {
            previous.close();
        }
    }

    @Override
    public void stopListening(UUID playerId) {
        Listener listener = listeners.remove(playerId);

        if (listener != null) {
            listener.close();
        }
    }

    // Every microphone packet, on voice chat's network thread. Never cancelled: nearby players still hear the player.
    public void onMicrophonePacket(MicrophonePacketEvent event) {
        VoicechatConnection sender = event.getSenderConnection();

        if (sender == null) {
            return;
        }

        Listener listener = listeners.get(sender.getPlayer().getUuid());

        if (listener != null) {
            listener.accept(event.getPacket().getOpusEncodedData());
        }
    }

    @Override
    public void play(UUID playerId, short[] samples, Runnable onStopped) {
        stopPlaying(playerId);

        VoicechatConnection connection = getApi().getConnectionOf(playerId);
        StaticAudioChannel channel = connection == null ? null : getApi().createStaticAudioChannel(UUID.randomUUID());

        if (channel == null) {
            onStopped.run();
            return;
        }

        channel.setCategory(VioVoicechatPlugin.VOLUME_CATEGORY);
        // Players in an isolated group still hear Vio.
        channel.setBypassGroupIsolation(true);
        channel.addTarget(connection);

        OpusEncoder encoder = getApi().createEncoder();
        AudioPlayer player = getApi().createAudioPlayer(channel, encoder, samples);

        player.setOnStopped(() -> {
            players.remove(playerId, player);

            if (!encoder.isClosed()) {
                encoder.close();
            }

            onStopped.run();
        });

        players.put(playerId, player);
        player.startPlaying();
    }

    @Override
    public void stopPlaying(UUID playerId) {
        AudioPlayer player = players.remove(playerId);

        if (player != null) {
            player.stopPlaying();
        }
    }

    // When voice chat's server stops, so no decoder or player outlives it.
    public void close() {
        List.copyOf(listeners.keySet()).forEach(this::stopListening);
        List.copyOf(players.keySet()).forEach(this::stopPlaying);
    }

    private static class Listener {

        private final OpusDecoder decoder;
        private final Consumer<short[]> frames;

        private boolean closed;

        private Listener(OpusDecoder decoder, Consumer<short[]> frames) {
            this.decoder = decoder;
            this.frames = frames;
        }

        // Packets are decoded on voice chat's thread while listening stops from the server thread,
        // so both are synchronized: a closed decoder must never be used.
        private synchronized void accept(byte[] data) {
            if (closed) {
                return;
            }

            // An empty packet ends a transmission; the next one starts fresh.
            if (data.length == 0) {
                decoder.resetState();
                return;
            }

            frames.accept(decoder.decode(data));
        }

        private synchronized void close() {
            closed = true;
            decoder.close();
        }
    }
}
