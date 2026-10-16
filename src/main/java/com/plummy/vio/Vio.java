package com.plummy.vio;

import com.mojang.logging.LogUtils;
import com.plummy.vio.access.AccessManager;
import com.plummy.vio.agent.Agent;
import com.plummy.vio.agent.ChatAgentListener;
import com.plummy.vio.agent.VioAgent;
import com.plummy.vio.command.VioCommand;
import com.plummy.vio.config.KeysConfig;
import com.plummy.vio.config.ServerConfig;
import com.plummy.vio.config.SystemMessageProvider;
import com.plummy.vio.provider.Provider;
import com.plummy.vio.provider.ProviderFactory;
import com.plummy.vio.storage.ConversationStorage;
import com.plummy.vio.storage.PlayerAccessStorage;
import com.plummy.vio.tool.BasicToolExecutor;
import com.plummy.vio.tool.ServerToolExecutor;
import net.minecraft.server.MinecraftServer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;

import java.net.http.HttpClient;

@Mod(Vio.MODID)
public class Vio {

    public static final String MODID = "vio";

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final Logger LOGGER = LogUtils.getLogger();

    private static Agent agent;
    private static Provider provider;
    private static AccessManager accessManager;

    private static ConversationStorage conversationStorage;
    private static PlayerAccessStorage playerAccessStorage;

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

        agent = new VioAgent(
                getProvider(),
                getConversationStorage(),
                accessManager,
                new BasicToolExecutor(),
                new ServerToolExecutor(server),
                SystemMessageProvider.load(),
                new ChatAgentListener()
        );
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        if (agent != null) {
            agent.shutdown();
            agent = null;
        }

        provider = null;
        conversationStorage = null;
        playerAccessStorage = null;
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        VioCommand.register(event.getDispatcher());
    }

    @EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {

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
}
