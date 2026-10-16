package com.plummy.vio.network;

import com.plummy.vio.Vio;
import com.plummy.vio.agent.Agent;
import com.plummy.vio.agent.SubmitResult;
import com.plummy.vio.client.network.ClientPayloadHandler;
import com.plummy.vio.message.Message;
import com.plummy.vio.network.payload.*;
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
        Instant time = Message.now();
        SubmitResult result = agent == null
                ? SubmitResult.UNAVAILABLE
                : agent.submit(player, request, time);

        // The client shows the request with the server's time, so it matches the stored one and the reply's.
        context.reply(new SubmitResultPayload(result, time));
    }
}
