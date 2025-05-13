package net.bandit.darkdoppelganger.client;

import net.bandit.darkdoppelganger.DarkDoppelgangerMod;
import net.bandit.darkdoppelganger.client.gui.DarkDoppelBossBar;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;

@EventBusSubscriber(modid = DarkDoppelgangerMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class GuiOverlayHandler {

    @SubscribeEvent
    public static void renderBossBar(CustomizeGuiOverlayEvent.BossEventProgress event) {
        DarkDoppelBossBar.renderBossBar(event);
    }
}
