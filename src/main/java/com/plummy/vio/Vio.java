package com.plummy.vio;

import com.mojang.logging.LogUtils;
import com.plummy.vio.conversation.Conversation;
import com.plummy.vio.conversation.PlayerConversation;
import com.plummy.vio.message.ConversationMessage;
import com.plummy.vio.message.PlayerRequestMessage;
import com.plummy.vio.provider.AgentProvider;
import com.plummy.vio.provider.OpenAIAgentProvider;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import org.slf4j.Logger;

import java.net.http.HttpClient;
import java.util.UUID;

@Mod(Vio.MODID)
public class Vio {

    public static final String MODID = "vio";

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final Logger LOGGER = LogUtils.getLogger();

    public Vio(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        NeoForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {

    }

    @SubscribeEvent
    public void onServerStarting(ServerAboutToStartEvent event) {
        AgentProvider provider = new OpenAIAgentProvider(
                "gpt-5-mini",
                "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"
        );

        Conversation conversation = new PlayerConversation(
                UUID.randomUUID(),
                "This is a test system message."
        );

        conversation.addMessage(
                new PlayerRequestMessage("Hello, OpenAI!")
        );

        ConversationMessage response = provider.request(conversation);
        getLogger().info(response.getContent());
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
}
