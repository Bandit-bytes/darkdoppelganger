package net.bandit.darkdoppelganger.event;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Pre event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.BOSS_EVENT_PROGRESS.id())) return;

        Map<UUID, LerpingBossEvent> bossBars = getBossBars();
        for (Map.Entry<UUID, LerpingBossEvent> entry : bossBars.entrySet()) {
            LerpingBossEvent boss = entry.getValue();
            if (boss.getName().getString().startsWith("Dark Doppelganger")) {
                renderCustomBossBar(boss, event.getGuiGraphics(), event.getWindow().getGuiScaledWidth());
                event.setCanceled(true);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<UUID, LerpingBossEvent> getBossBars() {
        try {
            var overlay = Minecraft.getInstance().gui.getBossOverlay();
            Field field = overlay.getClass().getDeclaredField("events");
            field.setAccessible(true);
            return (Map<UUID, LerpingBossEvent>) field.get(overlay);
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of();
        }
    }

    private static void renderCustomBossBar(LerpingBossEvent boss, GuiGraphics guiGraphics, int screenWidth) {
        Minecraft mc = Minecraft.getInstance();

        ResourceLocation frameTexture = new ResourceLocation("darkdoppelganger", "textures/gui/doppel_bossbar.png");
        ResourceLocation fillTexture = new ResourceLocation("darkdoppelganger", "textures/gui/doppel_fill.png");

        int frameWidth = 250;
        int frameHeight = 50;

        int barWidth = 182;
        int barHeight = 5;

        int x = (screenWidth - frameWidth) / 2;
        int y = 12;

        float progress = boss.getProgress();
        int filledWidth = (int)(progress * barWidth);

        if (filledWidth > 0) {
            int barX = x + 34;
            int barY = y + 12;
            guiGraphics.blit(fillTexture, barX, barY, 0, 0, filledWidth, barHeight, 256, 256);
        }

        guiGraphics.blit(frameTexture, x, y, 0, 0, frameWidth, frameHeight, frameWidth, frameHeight);
        Component name = boss.getName();
        int textX = (screenWidth / 2) - (mc.font.width(name) / 2);
        guiGraphics.drawString(mc.font, name, textX, y - 8, 0xAA44FF, true);
    }
}
