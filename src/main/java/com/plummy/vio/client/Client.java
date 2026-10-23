package com.plummy.vio.client;

import com.plummy.vio.Vio;
import com.plummy.vio.client.conversation.ClientConversation;
import com.plummy.vio.client.hud.IconRenderer;
import com.plummy.vio.client.screen.VioScreen;
import com.plummy.vio.voice.VoiceState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Style;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = Vio.MODID, value = Dist.CLIENT)
public class Client {

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(KeyMappings.OPEN);
        event.register(KeyMappings.TALK);
        event.register(KeyMappings.CANCEL);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientConversation conversation = ClientConversation.getInstance();

        while (KeyMappings.CANCEL.consumeClick()) {
            if (conversation.isCapturing()) {
                conversation.cancelCapture();
            }
        }

        while (KeyMappings.OPEN.consumeClick()) {
            if (minecraft.screen == null && minecraft.player != null) {
                minecraft.setScreen(new VioScreen());
            }
        }

        // Starts talking and sends early, with no window: the icon shows the talking, and the action bar any problem.
        while (KeyMappings.TALK.consumeClick()) {
            if (minecraft.screen != null || minecraft.player == null) {
                continue;
            }

            if (conversation.isCapturing()) {
                conversation.stopCapture();
            } else if (conversation.getVoiceState() != VoiceState.TRANSCRIBING) {
                conversation.startCapture();
            }
        }
    }

    @SubscribeEvent
    public static void onMouseButtonPressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof ChatScreen chat) || event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return;
        }

        Style style = VioScreen.getDetachedStyleAt(event.getMouseX(), event.getMouseY());

        if (style != null && style.getClickEvent() != null) {
            chat.handleComponentClicked(style);
            event.setCanceled(true);
            return;
        }

        if (IconRenderer.contains(chat.width, IconRenderer.CLOSED_SIZE, event.getMouseX(), event.getMouseY())) {
            Minecraft.getInstance().setScreen(new VioScreen());
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientConversation.getInstance().reset();
        VioScreen.clearDetached();
    }
}
