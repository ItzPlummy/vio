package com.plummy.vio;

import com.mojang.logging.LogUtils;
import com.plummy.vio.agent.Agent;
import com.plummy.vio.agent.VioAgent;
import com.plummy.vio.command.VioCommand;
import com.plummy.vio.config.Instructions;
import com.plummy.vio.config.KeysConfig;
import com.plummy.vio.config.ServerConfig;
import com.plummy.vio.core.access.AccessManager;
import com.plummy.vio.core.script.ScriptManager;
import com.plummy.vio.core.storage.ConversationStorage;
import com.plummy.vio.core.storage.PlayerAccessStorage;
import com.plummy.vio.core.storage.ScriptStorage;
import com.plummy.vio.core.tool.BasicToolExecutor;
import com.plummy.vio.core.tool.ServerToolExecutor;
import com.plummy.vio.listener.AgentListener;
import com.plummy.vio.listener.NetworkAgentListener;
import com.plummy.vio.provider.Provider;
import com.plummy.vio.provider.ProviderFactory;
import com.plummy.vio.voice.VoiceAgentListener;
import com.plummy.vio.voice.VoiceChat;
import com.plummy.vio.voice.VoiceManager;
import com.plummy.vio.voice.transformer.SpeechToTextTransformerFactory;
import com.plummy.vio.voice.transformer.TextToSpeechTransformerFactory;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;

import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.List;

@Mod(Vio.MODID)
public class Vio {

    public static final String MODID = "vio";
    public static final String VOICECHAT_MODID = "voicechat";

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
    private static final Logger LOGGER = LogUtils.getLogger();

    private static Agent agent;
    private static Provider provider;
    private static AccessManager accessManager;

    private static ConversationStorage conversationStorage;
    private static PlayerAccessStorage playerAccessStorage;

    private static VoiceManager voiceManager;
    private static ScriptManager scriptManager;

    // Set by the voice chat plugin while its server runs, from its own thread.
    private static volatile VoiceChat voiceChat;

    public Vio(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, ServerConfig.SPEC, MODID + "/server.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, KeysConfig.SPEC, MODID + "/keys.toml");

        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerStarting(ServerAboutToStartEvent event) {
        MinecraftServer server = event.getServer();

        provider = ProviderFactory.createProvider();
        conversationStorage = ConversationStorage.fromServer(server);
        playerAccessStorage = PlayerAccessStorage.fromServer(server);
        accessManager = new AccessManager(getPlayerAccessStorage());

        List<AgentListener> listeners = new ArrayList<>();
        listeners.add(new NetworkAgentListener());

        // Voice only works through Simple Voice Chat; without it, the speech model isn't even downloaded.
        if (ModList.get().isLoaded(VOICECHAT_MODID)) {
            voiceManager = new VoiceManager(SpeechToTextTransformerFactory.createTransformer(), TextToSpeechTransformerFactory.createTransformer());
            voiceManager.start();
            NeoForge.EVENT_BUS.register(voiceManager);
            listeners.add(new VoiceAgentListener(voiceManager));
        }

        agent = new VioAgent(
                getProvider(),
                getConversationStorage(),
                accessManager,
                new BasicToolExecutor(),
                new ServerToolExecutor(server),
                Instructions.load(),
                listeners
        );
    }

    // Unlike ServerAboutToStartEvent, the levels exist by now, which scripts may need.
    @SubscribeEvent
    public void onServerLevelsLoaded(ServerStartingEvent event) {
        if (ServerConfig.SCRIPTS_ENABLED.get()) {
            scriptManager = new ScriptManager(event.getServer(), ScriptStorage.fromServer(event.getServer()));
            scriptManager.load();
            NeoForge.EVENT_BUS.register(scriptManager);
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        if (agent != null) {
            agent.shutdown();
            agent = null;
        }

        if (scriptManager != null) {
            NeoForge.EVENT_BUS.unregister(scriptManager);
            scriptManager.shutdown();
            scriptManager = null;
        }

        if (voiceManager != null) {
            NeoForge.EVENT_BUS.unregister(voiceManager);
            voiceManager.shutdown();
            voiceManager = null;
        }

        provider = null;
        conversationStorage = null;
        playerAccessStorage = null;
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        VioCommand.register(event.getDispatcher());

        // Null on the first registration, which comes before scripts load; they add their commands themselves.
        if (scriptManager != null) {
            scriptManager.registerCommands(event.getDispatcher());
        }
    }

    public static HttpClient getHttpClient() {
        return HTTP_CLIENT;
    }

    public static Logger getLogger() {
        return LOGGER;
    }

    public static Agent getAgent() {
        return agent;
    }

    public static Provider getProvider() {
        return provider;
    }

    public static AccessManager getAccessManager() {
        return accessManager;
    }

    public static ConversationStorage getConversationStorage() {
        return conversationStorage;
    }

    public static PlayerAccessStorage getPlayerAccessStorage() {
        return playerAccessStorage;
    }

    public static VoiceManager getVoiceManager() {
        return voiceManager;
    }

    public static ScriptManager getScriptManager() {
        return scriptManager;
    }

    public static VoiceChat getVoiceChat() {
        return voiceChat;
    }

    public static void setVoiceChat(VoiceChat voiceChat) {
        Vio.voiceChat = voiceChat;
    }
}
