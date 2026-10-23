package com.plummy.vio.client.hud;

import com.plummy.vio.Vio;
import com.plummy.vio.client.screen.VioScreen;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber(modid = Vio.MODID, value = Dist.CLIENT)
public class IconLayer implements LayeredDraw.Layer {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Vio.MODID, "icon");

    private static final int BOSS_NAME_TOP = 3;
    private static final int BOSS_GAP = 2;

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.BOSS_OVERLAY, ID, new IconLayer());

        event.wrapLayer(VanillaGuiLayers.BOSS_OVERLAY, layer -> (graphics, deltaTracker) -> {
            graphics.pose().pushPose();
            // Below the icon as talking grows it, and below the status window while it shows; both are last frame's, as they're drawn after.
            int top = Math.max(IconRenderer.getBottom(IconRenderer.getGrownSize()), StatusWindow.getInstance().getBottom());
            graphics.pose().translate(0, top + BOSS_GAP - BOSS_NAME_TOP, 0);
            layer.render(graphics, deltaTracker);
            graphics.pose().popPose();
        });
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, @NotNull DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.options.hideGui || minecraft.screen instanceof VioScreen) {
            return;
        }

        if (VioScreen.renderDetached(graphics)) {
            return;
        }

        IconRenderer.render(graphics, IconRenderer.CLOSED_SIZE);
    }
}
