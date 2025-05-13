package net.bandit.darkdoppelganger.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.BossEvent;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;

public class DarkDoppelBossBar {

    private static final ResourceLocation BASE = ResourceLocation.fromNamespaceAndPath("darkdoppelganger", "textures/gui/doppel_bossbar.png");
    private static final ResourceLocation FILL = ResourceLocation.fromNamespaceAndPath("darkdoppelganger", "textures/gui/doppel_fill.png");

    private static final int FRAME_WIDTH = 250;
    private static final int FRAME_HEIGHT = 50;
    private static final int BAR_WIDTH = 182;
    private static final int BAR_HEIGHT = 5;
    private static final int BAR_OFFSET_X = 34;
    private static final int BAR_OFFSET_Y = 12;

    public static void renderBossBar(CustomizeGuiOverlayEvent.BossEventProgress event) {
        BossEvent boss = event.getBossEvent();

        if (!boss.getName().getString().startsWith("Dark Doppelganger")) return;

        event.setCanceled(true);

        GuiGraphics guiGraphics = event.getGuiGraphics();
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int y = event.getY();
        int x = (screenWidth - FRAME_WIDTH) / 2;

        float progress = boss.getProgress();
        int filledWidth = (int)(progress * BAR_WIDTH);

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        if (filledWidth > 0) {
            guiGraphics.blit(FILL, x + BAR_OFFSET_X, y + BAR_OFFSET_Y, 0, 0, filledWidth, BAR_HEIGHT, 256, 256);
        }
        guiGraphics.blit(BASE, x, y, 0, 0, FRAME_WIDTH, FRAME_HEIGHT, FRAME_WIDTH, FRAME_HEIGHT);

        Component name = boss.getName();
        int textX = screenWidth / 2 - Minecraft.getInstance().font.width(name) / 2;
        guiGraphics.drawString(Minecraft.getInstance().font, name, textX, y - 8, ChatFormatting.DARK_PURPLE.getColor());

        event.setIncrement(FRAME_HEIGHT);
    }
}
