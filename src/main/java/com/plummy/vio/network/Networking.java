package com.plummy.vio.network;

import com.plummy.vio.Vio;
import com.plummy.vio.network.payload.AgentResponsePayload;
import com.plummy.vio.network.payload.PlayerRequestPayload;
import com.plummy.vio.network.payload.ToolResponsePayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = Vio.MODID)
public class Networking {

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToServer(
                PlayerRequestPayload.getType(),
                PlayerRequestPayload.getStreamCodec(),
                (payload, context) -> {

                }
        );

        registrar.playToClient(
                ToolResponsePayload.getType(),
                ToolResponsePayload.getStreamCodec(),
                (payload, context) -> {

                }
        );

        registrar.playToClient(
                AgentResponsePayload.getType(),
                AgentResponsePayload.getStreamCodec(),
                (payload, context) -> {

                }
        );
    }
}
