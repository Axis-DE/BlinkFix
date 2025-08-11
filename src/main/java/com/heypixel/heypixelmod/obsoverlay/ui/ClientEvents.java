package com.heypixel.heypixelmod.obsoverlay.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "naven", value = Dist.CLIENT)
public class ClientEvents {
    private static final ResourceLocation CUSTOM_BG = new ResourceLocation("naven", "textures/gui/custom_bg.png");

    @SubscribeEvent
    public static void onRenderTitleScreenPost(ScreenEvent.Render.Post event) {
        if (event.getScreen() instanceof TitleScreen screen) {
            GuiGraphics guiGraphics = event.getGuiGraphics();
            RenderSystem.setShaderTexture(0, CUSTOM_BG);
            guiGraphics.blit(CUSTOM_BG, 0, 0, 0, 0,
                    guiGraphics.guiWidth(), guiGraphics.guiHeight(),
                    guiGraphics.guiWidth(), guiGraphics.guiHeight());
        }
    }
}