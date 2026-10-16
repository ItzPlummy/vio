package com.plummy.vio.client;

import com.plummy.vio.Vio;
import com.plummy.vio.client.hud.VioIcon;
import com.plummy.vio.client.screen.VioScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = Vio.MODID, value = Dist.CLIENT)
public class VioClient {

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(VioKeyMappings.OPEN);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();

        // Handled on tick rather than on key press, so the typed 'v' never reaches the new screen.
        while (VioKeyMappings.OPEN.consumeClick()) {
            if (minecraft.screen == null && minecraft.player != null) {
                minecraft.setScreen(new VioScreen());
            }
        }
    }

    @SubscribeEvent
    public static void onMouseButtonPressed(ScreenEvent.MouseButtonPressed.Pre event) {
        // With the vanilla chat open, the mouse is free, so the HUD icon can be clicked.
        if (event.getScreen() instanceof ChatScreen chat
                && event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT
                && VioIcon.contains(chat.width, VioIcon.CLOSED_SIZE, event.getMouseX(), event.getMouseY())) {
            Minecraft.getInstance().setScreen(new VioScreen());
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        // The next server sends its own history on join.
        ClientChatHistory.getInstance().reset();
        VioScreen.clearDetached();
    }
}
