package com.plummy.vio.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public final class VioKeyMappings {

    public static final String CATEGORY = "key.categories.vio";

    public static final KeyMapping OPEN = new KeyMapping(
            "key.vio.open",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            CATEGORY
    );

    private VioKeyMappings() {
    }
}
