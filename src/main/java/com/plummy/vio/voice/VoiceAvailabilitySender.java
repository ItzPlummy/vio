package com.plummy.vio.voice;

import com.plummy.vio.Vio;
import com.plummy.vio.network.Networking;
import com.plummy.vio.network.payload.VoiceAvailabilityPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.UUID;

@EventBusSubscriber(modid = Vio.MODID)
public class VoiceAvailabilitySender {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            send(player);
        }
    }

    public static VoiceAvailability getAvailability(ServerPlayer player) {
        VoiceChat voiceChat = Vio.getVoiceChat();

        if (voiceChat == null || Vio.getVoiceManager() == null) {
            return VoiceAvailability.UNSUPPORTED;
        }

        return voiceChat.getAvailability(player.getUUID());
    }

    public static void send(ServerPlayer player) {
        VoiceAvailability availability = getAvailability(player);

        Vio.getLogger().debug("Voice availability of %s is %s".formatted(player.getGameProfile().getName(), availability));
        Networking.send(player, new VoiceAvailabilityPayload(availability));
    }

    // Voice chat events arrive on its own threads, so sending is moved to the server thread.
    public static void sendLater(UUID playerId) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();

        if (server == null || !server.isRunning()) {
            return;
        }

        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);

            if (player != null) {
                send(player);
            }
        });
    }

    public static void sendAllLater() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();

        if (server == null || !server.isRunning()) {
            return;
        }

        server.execute(() -> server.getPlayerList().getPlayers().forEach(VoiceAvailabilitySender::send));
    }
}
