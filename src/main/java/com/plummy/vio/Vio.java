package com.plummy.vio;

import com.mojang.logging.LogUtils;
import com.plummy.vio.agent.Agent;
import com.plummy.vio.agent.ChatAgentListener;
import com.plummy.vio.agent.VioAgent;
import com.plummy.vio.command.VioCommand;
import com.plummy.vio.config.KeysConfig;
import com.plummy.vio.config.ServerConfig;
import com.plummy.vio.config.SystemPromptConfig;
import com.plummy.vio.provider.OpenAIAgentProvider;
import com.plummy.vio.registry.ToolRegistry;
import com.plummy.vio.storage.ConversationStorage;
import com.plummy.vio.tool.ToolExecutor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;

import java.net.http.HttpClient;
import java.util.List;

@Mod(Vio.MODID)
public class Vio {

    public static final String MODID = "vio";

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final Logger LOGGER = LogUtils.getLogger();

    private static Agent agent;

    public Vio(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        modContainer.registerConfig(ModConfig.Type.COMMON, ServerConfig.SPEC, MODID + "/server.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, KeysConfig.SPEC, MODID + "/keys.toml");

        NeoForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {

    }

    @SubscribeEvent
    public void onServerStarting(ServerAboutToStartEvent event) {
        SystemPromptConfig.createDefaultIfMissing();

        if (!ServerConfig.PROVIDER.get().equals("openai")) {
            LOGGER.warn("Provider '{}' is not implemented yet, using openai", ServerConfig.PROVIDER.get());
        }

        agent = new VioAgent(
                new OpenAIAgentProvider(ServerConfig.OPENAI_MODEL.get(), KeysConfig.OPENAI.get()),
                ConversationStorage.fromServer(event.getServer()),
                new ToolExecutor(),
                SystemPromptConfig::load,
                ToolRegistry.getAll(),
                new ChatAgentListener()
        );
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        if (agent != null) {
            agent.shutdown();
            agent = null;
        }
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

    public static Agent getAgent() {
        return agent;
    }

    public static HttpClient getHttpClient() {
        return HTTP_CLIENT;
    }

    public static Logger getLogger() {
        return LOGGER;
    }
}
