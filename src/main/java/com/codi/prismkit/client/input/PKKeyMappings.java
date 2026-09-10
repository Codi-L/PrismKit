package com.codi.prismkit.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.common.util.Lazy;
import org.lwjgl.glfw.GLFW;

public final class PKKeyMappings {

    public static final String CATEGORY = "key.categories.prismkit";

    public static final Lazy<KeyMapping> OPEN_CURVE_EDITOR = Lazy.of(() -> new KeyMapping(
            "key.prismkit.open.curve.editor",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            CATEGORY
    ));

    private PKKeyMappings() {}

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(OPEN_CURVE_EDITOR.get());
    }
}
