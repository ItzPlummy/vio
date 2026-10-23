package com.plummy.vio.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

public final class KeyMappings {

    public static final String CATEGORY = "key.categories.vio";

    public static final KeyMapping OPEN = new KeyMapping(
            "key.vio.open",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            CATEGORY
    );

    // Starts talking, with the window open but not focused; pressed again, sends what was said.
    public static final KeyMapping TALK = new KeyMapping(
            "key.vio.talk",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            CATEGORY
    );

    // Drops what's being said without sending it. Held Shift picks this over TALK.
    public static final KeyMapping CANCEL = new KeyMapping(
            "key.vio.cancel",
            KeyConflictContext.IN_GAME,
            KeyModifier.SHIFT,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            CATEGORY
    );

    private KeyMappings() {
    }
}
