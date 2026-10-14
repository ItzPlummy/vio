package com.plummy.vio;

import com.mojang.logging.LogUtils;
import com.plummy.vio.config.KeysConfig;
import com.plummy.vio.config.ServerConfig;
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
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import org.slf4j.Logger;

import java.net.http.HttpClient;

@Mod(Vio.MODID)
public class Vio {

    public static final String MODID = "vio";

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final Logger LOGGER = LogUtils.getLogger();

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
