package com.plummy.vio.voice;

import com.plummy.vio.Vio;
import com.plummy.vio.agent.Agent;
import com.plummy.vio.agent.SubmitResult;
import com.plummy.vio.config.ServerConfig;
import com.plummy.vio.core.access.AccessLevel;
import com.plummy.vio.core.message.Message;
import com.plummy.vio.network.Networking;
import com.plummy.vio.network.payload.AgentErrorPayload;
import com.plummy.vio.network.payload.SubmitResultPayload;
import com.plummy.vio.network.payload.VoiceStatePayload;
import com.plummy.vio.network.payload.VoiceTranscriptPayload;
import com.plummy.vio.voice.transformer.SpeechToTextTransformer;
import com.plummy.vio.voice.transformer.TextToSpeechTransformer;
import com.plummy.vio.voice.transformer.Transcript;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

// Captures, transcription results and replies all change state on the server thread; only the transformers run on
// their workers, and only microphone frames arrive on voice chat's thread, straight into their CaptureSession.
public class VoiceManager {

    private static final long SHUTDOWN_TIMEOUT_SECONDS = 5;

    private final SpeechToTextTransformer speechToText;
    private final TextToSpeechTransformer textToSpeech;

    // Separate, so a slow text to speech request never holds up transcription.
    private final ExecutorService speechToTextWorker = createWorker("vio_speech_to_text");
    private final ExecutorService textToSpeechWorker = createWorker("vio_text_to_speech");

    // LOADING until speech to text is loaded, then AVAILABLE, or UNSUPPORTED if loading failed.
    private volatile VoiceAvailability availability = VoiceAvailability.LOADING;

    private final Map<UUID, CaptureSession> sessions = new HashMap<>();
    // Players whose current request was spoken, so its reply is spoken too, in the request's language if it's known.
    private final Map<UUID, String> awaitingReply = new HashMap<>();
    private final Set<UUID> speakingPlayers = new HashSet<>();

    public VoiceManager(SpeechToTextTransformer speechToText, TextToSpeechTransformer textToSpeech) {
        this.speechToText = speechToText;
        this.textToSpeech = textToSpeech;
    }

    public VoiceAvailability getAvailability() {
        return availability;
    }

    // Loading may download the model first, so it runs on the worker that later uses it.
    public void start() {
        speechToTextWorker.execute(() -> {
            long start = System.nanoTime();

            try {
                speechToText.load();
                availability = VoiceAvailability.AVAILABLE;
                Vio.getLogger().info("Speech to text loaded in {} ms", (System.nanoTime() - start) / 1_000_000);
            } catch (RuntimeException e) {
                availability = VoiceAvailability.UNSUPPORTED;
                Vio.getLogger().error("Speech to text failed to load, voice is unavailable", e);
            }

            VoiceAvailabilitySender.sendAllLater();
        });
    }

    public void shutdown() {
        List.copyOf(sessions.keySet()).forEach(this::endCapture);
        List.copyOf(speakingPlayers).forEach(this::stopSpeaking);
        awaitingReply.clear();

        textToSpeechWorker.shutdownNow();
        speechToTextWorker.shutdownNow();

        // A model may still be in use by a running transcription or synthesis, so it's released only once that ends.
        try {
            if (speechToTextWorker.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                speechToText.release();
            } else {
                Vio.getLogger().warn("Speech to text didn't stop in time, its model isn't released");
            }

            if (textToSpeechWorker.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                textToSpeech.release();
            } else {
                Vio.getLogger().warn("Text to speech didn't stop in time, its voices aren't released");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Anything but ACCEPTED is reported to the player like a rejected text request.
    public SubmitResult startCapture(ServerPlayer player) {
        UUID playerId = player.getUUID();
        VoiceChat voiceChat = Vio.getVoiceChat();
        Agent agent = Vio.getAgent();

        if (voiceChat == null || agent == null || VoiceAvailabilitySender.getAvailability(player) != VoiceAvailability.AVAILABLE) {
            return SubmitResult.UNAVAILABLE;
        }

        if (Vio.getAccessManager().getAccessLevel(playerId) == AccessLevel.NONE) {
            return SubmitResult.DENIED;
        }

        if (agent.isBusy(playerId)) {
            return SubmitResult.BUSY;
        }

        // Talking again cuts Vio off, and replaces a capture that somehow didn't end.
        stopSpeaking(playerId);
        endCapture(playerId);

        CaptureSession session = new CaptureSession(
                playerId,
                Util.getMillis(),
                Math.round(ServerConfig.SILENCE_SECONDS.get() * 1000),
                ServerConfig.MAX_CAPTURE_SECONDS.get() * 1000L
        );

        sessions.put(playerId, session);
        voiceChat.startListening(playerId, frame -> session.accept(frame, Util.getMillis()));
        sendState(player, VoiceState.LISTENING);

        return SubmitResult.ACCEPTED;
    }

    // The player stopped talking early: send what was said.
    public void stopCapture(ServerPlayer player) {
        CaptureSession session = sessions.get(player.getUUID());

        if (session != null && isListening(session)) {
            finish(player.getServer(), session);
        }
    }

    public void cancelCapture(ServerPlayer player) {
        if (endCapture(player.getUUID())) {
            sendState(player, VoiceState.IDLE);
        }
    }

    // From the agent listener, on the server thread.
    public void speakReply(ServerPlayer player, String reply) {
        UUID playerId = player.getUUID();
        MinecraftServer server = player.getServer();

        if (!awaitingReply.containsKey(playerId) || server == null) {
            return;
        }

        String language = awaitingReply.remove(playerId);
        String text = SpokenText.fromMarkdown(reply);

        // Nothing to say, e.g. only code: the client still waits for speech, so it's told there won't be any.
        if (text.isBlank()) {
            sendState(player, VoiceState.IDLE);
            return;
        }

        textToSpeechWorker.execute(() -> {
            short[] audio;

            try {
                audio = textToSpeech.synthesize(text, language);
            } catch (RuntimeException e) {
                Vio.getLogger().error("Text to speech for {} failed", playerId, e);
                server.execute(() -> sendError(server, playerId, "speech"));
                return;
            }

            server.execute(() -> play(server, playerId, audio));
        });
    }

    public void dropReply(ServerPlayer player) {
        awaitingReply.remove(player.getUUID());
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (sessions.isEmpty()) {
            return;
        }

        long nowMillis = Util.getMillis();

        for (CaptureSession session : List.copyOf(sessions.values())) {
            update(event.getServer(), session, nowMillis);
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID playerId = event.getEntity().getUUID();

        endCapture(playerId);
        stopSpeaking(playerId);
        awaitingReply.remove(playerId);
    }

    private void update(MinecraftServer server, CaptureSession session, long nowMillis) {
        if (!isListening(session)) {
            return;
        }

        if (Vio.getVoiceChat() == null) {
            endCapture(session.getPlayerId());
            sendState(server, session.getPlayerId(), VoiceState.IDLE);
            return;
        }

        if (session.isFinished(nowMillis)) {
            finish(server, session);
            return;
        }

        if (session.getState() == VoiceState.LISTENING && session.isWithoutAudio(nowMillis)) {
            session.setState(VoiceState.NO_AUDIO);
            sendState(server, session.getPlayerId(), VoiceState.NO_AUDIO);
        } else if (session.getState() == VoiceState.NO_AUDIO && session.hasAudio()) {
            session.setState(VoiceState.LISTENING);
            sendState(server, session.getPlayerId(), VoiceState.LISTENING);
        }
    }

    // The whole capture is transcribed at once: with all of it as context, transcription is at its best.
    private void finish(MinecraftServer server, CaptureSession session) {
        session.setState(VoiceState.TRANSCRIBING);
        sendState(server, session.getPlayerId(), VoiceState.TRANSCRIBING);

        VoiceChat voiceChat = Vio.getVoiceChat();

        if (voiceChat != null) {
            voiceChat.stopListening(session.getPlayerId());
        }

        // Nothing was said, so there's nothing worth transcribing, or paying for.
        if (!session.hasSpeech()) {
            submit(server, session, new Transcript("", null), false);
            return;
        }

        short[] speech = session.getSpeech();
        ServerPlayer player = server.getPlayerList().getPlayer(session.getPlayerId());
        String gameLanguage = player == null ? null : getGameLanguage(player);

        speechToTextWorker.execute(() -> {
            Transcript transcript;
            boolean failed = false;

            try {
                transcript = speechToText.transcribe(speech, gameLanguage);
            } catch (RuntimeException e) {
                Vio.getLogger().error("Speech to text for {} failed", session.getPlayerId(), e);
                transcript = new Transcript("", null);
                failed = true;
            }

            Transcript result = transcript;
            boolean transcriptionFailed = failed;

            server.execute(() -> submit(server, session, result, transcriptionFailed));
        });
    }

    private void submit(MinecraftServer server, CaptureSession session, Transcript transcript, boolean failed) {
        UUID playerId = session.getPlayerId();
        String text = transcript.text();

        if (sessions.get(playerId) != session) {
            return;
        }

        sessions.remove(playerId);

        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        Agent agent = Vio.getAgent();

        if (player == null) {
            return;
        }

        // The client drops an unsent capture's bubble on IDLE, so a sent one gets its result first.
        if (text.isBlank()) {
            sendState(player, VoiceState.IDLE);
            sendError(server, playerId, failed ? "transcription" : "empty");
            return;
        }

        Instant time = Message.now();
        SubmitResult result = agent == null ? SubmitResult.UNAVAILABLE : agent.submit(player, text, time, true);

        if (result == SubmitResult.ACCEPTED) {
            awaitingReply.put(playerId, transcript.language());
        }

        // The bubble shows what was understood before it's settled, like a typed request.
        Networking.send(player, new VoiceTranscriptPayload(text));
        Networking.send(player, new SubmitResultPayload(result, time));
        sendState(player, VoiceState.IDLE);
    }

    private void play(MinecraftServer server, UUID playerId, short[] audio) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        VoiceChat voiceChat = Vio.getVoiceChat();

        // Talking again while the reply was synthesized means it's no longer wanted.
        if (player == null || sessions.containsKey(playerId)) {
            return;
        }

        if (voiceChat == null) {
            sendState(player, VoiceState.IDLE);
            return;
        }

        speakingPlayers.add(playerId);
        sendState(player, VoiceState.SPEAKING);
        voiceChat.play(playerId, audio, () -> server.execute(() -> onSpeechStopped(server, playerId)));
    }

    private void onSpeechStopped(MinecraftServer server, UUID playerId) {
        // Already gone if it was cut off, and then a capture's state is the one that matters.
        if (speakingPlayers.remove(playerId)) {
            sendState(server, playerId, VoiceState.IDLE);
        }
    }

    private void stopSpeaking(UUID playerId) {
        VoiceChat voiceChat = Vio.getVoiceChat();

        if (speakingPlayers.remove(playerId) && voiceChat != null) {
            voiceChat.stopPlaying(playerId);
        }
    }

    // Drops the capture without sending it. Returns whether there was one.
    private boolean endCapture(UUID playerId) {
        CaptureSession session = sessions.remove(playerId);
        VoiceChat voiceChat = Vio.getVoiceChat();

        if (session != null && voiceChat != null) {
            voiceChat.stopListening(playerId);
        }

        return session != null;
    }

    // The client's language setting, e.g. "ru_ru", as an ISO 639-1 code; null for joke languages like "lol_us".
    private static String getGameLanguage(ServerPlayer player) {
        String language = player.clientInformation().language();
        String code = language.substring(0, Math.max(0, language.indexOf('_')));

        return Arrays.asList(Locale.getISOLanguages()).contains(code) ? code : null;
    }

    private static boolean isListening(CaptureSession session) {
        VoiceState state = session.getState();

        return state == VoiceState.LISTENING || state == VoiceState.NO_AUDIO;
    }

    private static void sendState(ServerPlayer player, VoiceState state) {
        Networking.send(player, new VoiceStatePayload(state));
    }

    private static void sendState(MinecraftServer server, UUID playerId, VoiceState state) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);

        if (player != null) {
            sendState(player, state);
        }
    }

    // Translated on the client, in the player's language.
    private static void sendError(MinecraftServer server, UUID playerId, String name) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);

        if (player != null) {
            Networking.send(player, new AgentErrorPayload(Component.translatable("vio.voice.error." + name)));
        }
    }

    private static ExecutorService createWorker(String name) {
        return Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, name);
            thread.setDaemon(true);
            return thread;
        });
    }
}
