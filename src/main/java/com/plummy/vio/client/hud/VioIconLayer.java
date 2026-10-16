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
public class VioIconLayer implements LayeredDraw.Layer {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Vio.MODID, "icon");

    // Vanilla draws the first boss bar's name at y = 3.
    private static final int BOSS_NAME_TOP = 3;
    private static final int BOSS_GAP = 2;

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.BOSS_OVERLAY, ID, new VioIconLayer());

        // The icon owns the top center, so the boss bars move down below it.
        event.wrapLayer(VanillaGuiLayers.BOSS_OVERLAY, layer -> (graphics, deltaTracker) -> {
            graphics.pose().pushPose();
            graphics.pose().translate(0, VioIcon.getBottom(VioIcon.CLOSED_SIZE) + BOSS_GAP - BOSS_NAME_TOP, 0);
            layer.render(graphics, deltaTracker);
            graphics.pose().popPose();
        });
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, @NotNull DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();

        // The open screen draws the large icon itself.
        if (minecraft.options.hideGui || minecraft.screen instanceof VioScreen) {
            return;
        }

        // While the closed window is still shown, its icon stands in for this one.
        if (VioScreen.renderDetached(graphics)) {
            return;
        }

        VioIcon.render(graphics, VioIcon.CLOSED_SIZE);
    }
}
