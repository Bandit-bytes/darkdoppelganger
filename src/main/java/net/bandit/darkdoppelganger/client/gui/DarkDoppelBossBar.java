package net.bandit.darkdoppelganger.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;

public class DarkDoppelBossBar {

    public static final DarkDoppelBossBar INSTANCE = new DarkDoppelBossBar();

    private static final ResourceLocation FRAME = new ResourceLocation("darkdoppelganger", "textures/gui/doppel_bossbar.png");
    private static final ResourceLocation FILL = new ResourceLocation("darkdoppelganger", "textures/gui/doppel_fill.png");

    private static final int FRAME_WIDTH = 250;
    private static final int FRAME_HEIGHT = 50;
    private static final int BAR_WIDTH = 182;
    private static final int BAR_HEIGHT = 5;
    private static final int BAR_OFFSET_X = 34;
    private static final int BAR_OFFSET_Y = 12;

    private DarkDoppelBossBar() {}

    public void renderBossBar(CustomizeGuiOverlayEvent.BossEventProgress event) {
        GuiGraphics guiGraphics = event.getGuiGraphics();
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int x = (screenWidth - FRAME_WIDTH) / 2;
        int y = event.getY();

        float progress = event.getBossEvent().getProgress();
        int filled = (int)(progress * BAR_WIDTH);

        event.setCanceled(true);

        if (filled > 0) {
            guiGraphics.blit(FILL, x + BAR_OFFSET_X, y + BAR_OFFSET_Y, 0, 0, filled, BAR_HEIGHT, 256, 256);
        }

        guiGraphics.blit(FRAME, x, y, 0, 0, FRAME_WIDTH, FRAME_HEIGHT, FRAME_WIDTH, FRAME_HEIGHT);

        Component name = event.getBossEvent().getName();
        int textX = (screenWidth / 2) - (Minecraft.getInstance().font.width(name) / 2);
        guiGraphics.drawString(Minecraft.getInstance().font, name, textX, y - 8, 0xAA44FF);

        event.setIncrement(FRAME_HEIGHT);
    }
}
