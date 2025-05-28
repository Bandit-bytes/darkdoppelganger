package net.bandit.darkdoppelganger.client;

import net.bandit.darkdoppelganger.DarkDoppelgangerMod;
import net.bandit.darkdoppelganger.entity.DarkDoppelgangerMinionEntity;
import net.bandit.darkdoppelganger.entity.renderer.DarkDoppelgangerMinionRenderer;
import net.bandit.darkdoppelganger.entity.renderer.PortalJoinRenderer;
import net.bandit.darkdoppelganger.entity.renderer.PortalLeaveRenderer;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.bandit.darkdoppelganger.entity.EntityRegistry;
import net.bandit.darkdoppelganger.entity.renderer.DarkDoppelgangerRenderer;

@Mod.EventBusSubscriber(modid = DarkDoppelgangerMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientModEvents {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // Register the entity renderer
        EntityRenderers.register(EntityRegistry.DARK_DOPPELGANGER.get(), DarkDoppelgangerRenderer::new);
        EntityRenderers.register(EntityRegistry.DARK_DOPPELGANGER_MINION.get(), DarkDoppelgangerMinionRenderer::new);
        EntityRenderers.register(EntityRegistry.PORTAL_JOIN_ENTITY.get(), PortalJoinRenderer::new);
        EntityRenderers.register(EntityRegistry.PORTAL_LEAVE_ENTITY.get(), PortalLeaveRenderer::new);
    }
}
