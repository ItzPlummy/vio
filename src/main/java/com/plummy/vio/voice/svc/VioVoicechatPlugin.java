package com.plummy.vio.voice.svc;

import com.plummy.vio.Vio;
import com.plummy.vio.voice.VoiceAvailabilitySender;
import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.VolumeCategory;
import de.maxhenkel.voicechat.api.events.*;

// Found and created by Simple Voice Chat itself, so this package is only ever loaded when it's installed.
@ForgeVoicechatPlugin
public class VioVoicechatPlugin implements VoicechatPlugin {

    public static final String VOLUME_CATEGORY = Vio.MODID;

    // Set from voice chat's own threads, and read for every microphone packet.
    private volatile SimpleVoiceChat voiceChat;

    @Override
    public String getPluginId() {
        return Vio.MODID;
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(VoicechatServerStartedEvent.class, this::onServerStarted);
        registration.registerEvent(VoicechatServerStoppedEvent.class, this::onServerStopped);
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophonePacket);
        registration.registerEvent(PlayerConnectedEvent.class, event -> VoiceAvailabilitySender.sendLater(event.getConnection().getPlayer().getUuid()));
        registration.registerEvent(PlayerDisconnectedEvent.class, event -> VoiceAvailabilitySender.sendLater(event.getPlayerUuid()));
        registration.registerEvent(PlayerStateChangedEvent.class, event -> VoiceAvailabilitySender.sendLater(event.getPlayerUuid()));
    }

    private void onServerStarted(VoicechatServerStartedEvent event) {
        VoicechatServerApi api = event.getVoicechat();

        VolumeCategory category = api.volumeCategoryBuilder()
                .setId(VOLUME_CATEGORY)
                .setName("Vio")
                .setNameTranslationKey("vio.voice.category")
                .setDescription("Vio's spoken replies")
                .setDescriptionTranslationKey("vio.voice.category.description")
                .build();

        api.registerVolumeCategory(category);

        voiceChat = new SimpleVoiceChat(api);
        Vio.setVoiceChat(voiceChat);
        Vio.getLogger().info("Simple Voice Chat is running, voice is supported");
        VoiceAvailabilitySender.sendAllLater();
    }

    private void onServerStopped(VoicechatServerStoppedEvent event) {
        SimpleVoiceChat stopped = voiceChat;

        voiceChat = null;
        Vio.setVoiceChat(null);

        if (stopped != null) {
            stopped.close();
        }

        VoiceAvailabilitySender.sendAllLater();
    }

    private void onMicrophonePacket(MicrophonePacketEvent event) {
        SimpleVoiceChat current = voiceChat;

        if (current != null) {
            current.onMicrophonePacket(event);
        }
    }
}
