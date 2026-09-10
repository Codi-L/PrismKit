package com.codi.prismkit.client.ui;

import com.codi.prismkit.PrismKit;
import com.codi.prismkit.client.input.PKKeyMappings;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.Logger;

public final class CurveEditorScreen extends Screen {

    private final Screen parent;
    private static final Logger LOGGER = LogUtils.getLogger();

    public CurveEditorScreen(Screen parent) {
        super(Component.translatable("screen.prismkit.curve_editor"));
        this.parent = parent;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @EventBusSubscriber(modid = PrismKit.MOD_ID, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            while (PKKeyMappings.OPEN_CURVE_EDITOR.get().consumeClick()) {
                LOGGER.debug("打开UI");
                Minecraft minecraft = Minecraft.getInstance();

                if (minecraft.screen == null) {
                    minecraft.setScreen(new CurveEditorScreen(null));
                }
            }
        }
    }
}
