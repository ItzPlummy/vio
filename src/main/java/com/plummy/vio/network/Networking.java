package com.plummy.vio.network;

import com.plummy.vio.Vio;
import com.plummy.vio.agent.Agent;
import com.plummy.vio.agent.SubmitResult;
import com.plummy.vio.client.network.ClientPayloadHandler;
import com.plummy.vio.network.payload.*;
import com.plummy.vio.voice.VoiceManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@EventBusSubscriber(modid = Vio.MODID)
public class Networking {

    public static final StreamCodec<ByteBuf, String> TEXT_CODEC = ByteBufCodecs.stringUtf8(65536);
    public static final StreamCodec<ByteBuf, Instant> TIME_CODEC = ByteBufCodecs.VAR_LONG.map(Instant::ofEpochMilli, Instant::toEpochMilli);

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1").optional();

        registrar.playToServer(
                PlayerRequestPayload.getType(),
                PlayerRequestPayload.getStreamCodec(),
                Networking::handlePlayerRequest
        );

        registrar.playToClient(
                SubmitResultPayload.getType(),
                SubmitResultPayload.getStreamCodec(),
                (payload, context) -> ClientPayloadHandler.handleSubmitResult(payload)
        );

        registrar.playToClient(
                AgentStatusPayload.getType(),
                AgentStatusPayload.getStreamCodec(),
                (payload, context) -> ClientPayloadHandler.handleStatus(payload)
        );

        registrar.playToClient(
                AgentResponsePayload.getType(),
                AgentResponsePayload.getStreamCodec(),
                (payload, context) -> ClientPayloadHandler.handleResponse(payload)
        );

        registrar.playToClient(
                AgentErrorPayload.getType(),
                AgentErrorPayload.getStreamCodec(),
                (payload, context) -> ClientPayloadHandler.handleError(payload)
        );

        registrar.playToClient(
                HistoryPayload.getType(),
                HistoryPayload.getStreamCodec(),
                (payload, context) -> ClientPayloadHandler.handleHistory(payload)
        );

        registrar.playToClient(
                ClearHistoryPayload.getType(),
                ClearHistoryPayload.getStreamCodec(),
                (payload, context) -> ClientPayloadHandler.handleClearHistory()
        );

        registrar.playToClient(
                VoiceAvailabilityPayload.getType(),
                VoiceAvailabilityPayload.getStreamCodec(),
                (payload, context) -> ClientPayloadHandler.handleVoiceAvailability(payload)
        );

        registrar.playToServer(
                StartCapturePayload.getType(),
                StartCapturePayload.getStreamCodec(),
                (payload, context) -> handleStartCapture(context)
        );

        registrar.playToServer(
                StopCapturePayload.getType(),
                StopCapturePayload.getStreamCodec(),
                (payload, context) -> handleStopCapture(context)
        );

        registrar.playToServer(
                CancelCapturePayload.getType(),
                CancelCapturePayload.getStreamCodec(),
                (payload, context) -> handleCancelCapture(context)
        );

        registrar.playToServer(
                ResetConversationPayload.getType(),
                ResetConversationPayload.getStreamCodec(),
                (payload, context) -> handleResetConversation(context)
        );

        registrar.playToClient(
                VoiceStatePayload.getType(),
                VoiceStatePayload.getStreamCodec(),
                (payload, context) -> ClientPayloadHandler.handleVoiceState(payload)
        );

        registrar.playToClient(
                VoiceTranscriptPayload.getType(),
                VoiceTranscriptPayload.getStreamCodec(),
                (payload, context) -> ClientPayloadHandler.handleVoiceTranscript(payload)
        );
    }

    public static boolean hasClient(ServerPlayer player) {
        return player.connection.hasChannel(PlayerRequestPayload.getType());
    }

    public static void send(ServerPlayer player, CustomPacketPayload payload) {
        if (hasClient(player)) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    private static void handlePlayerRequest(PlayerRequestPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        String request = payload.getRequest().strip();

        if (request.isEmpty()) {
            return;
        }

        Agent agent = Vio.getAgent();
        Instant time = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        SubmitResult result = agent == null
                ? SubmitResult.UNAVAILABLE
                : agent.submit(player, request, time, false);

        // The client shows the request with the server's time, so it matches the stored one and the reply's.
        context.reply(new SubmitResultPayload(result, time));
    }

    private static void handleResetConversation(IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        Agent agent = Vio.getAgent();

        if (agent == null) {
            context.reply(new SubmitResultPayload(SubmitResult.UNAVAILABLE, Instant.now().truncatedTo(ChronoUnit.MILLIS)));
            return;
        }

        if (!agent.reset(player.getUUID())) {
            context.reply(new SubmitResultPayload(SubmitResult.BUSY, Instant.now().truncatedTo(ChronoUnit.MILLIS)));
            return;
        }

        context.reply(ClearHistoryPayload.getInstance());
    }

    private static void handleStartCapture(IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        VoiceManager voiceManager = Vio.getVoiceManager();
        SubmitResult result = voiceManager == null
                ? SubmitResult.UNAVAILABLE
                : voiceManager.startCapture(player);

        // An accepted capture reports through its voice state; the request itself is submitted once it ends.
        if (result != SubmitResult.ACCEPTED) {
            context.reply(new SubmitResultPayload(result, Instant.now().truncatedTo(ChronoUnit.MILLIS)));
        }
    }

    private static void handleStopCapture(IPayloadContext context) {
        VoiceManager voiceManager = Vio.getVoiceManager();

        if (context.player() instanceof ServerPlayer player && voiceManager != null) {
            voiceManager.stopCapture(player);
        }
    }

    private static void handleCancelCapture(IPayloadContext context) {
        VoiceManager voiceManager = Vio.getVoiceManager();

        if (context.player() instanceof ServerPlayer player && voiceManager != null) {
            voiceManager.cancelCapture(player);
        }
    }
}
