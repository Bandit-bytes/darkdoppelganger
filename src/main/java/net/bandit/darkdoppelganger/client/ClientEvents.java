package net.bandit.darkdoppelganger.client;

import net.bandit.darkdoppelganger.DarkDoppelgangerMod;
import net.bandit.darkdoppelganger.entity.EnderDaggerRenderer;
import net.bandit.darkdoppelganger.entity.renderer.DarkDoppelgangerMinionRenderer;
import net.bandit.darkdoppelganger.entity.renderer.DarkDoppelgangerRenderer;
import net.bandit.darkdoppelganger.entity.renderer.PortalJoinRenderer;
import net.bandit.darkdoppelganger.entity.renderer.PortalLeaveRenderer;
import net.bandit.darkdoppelganger.registry.EntityRegistry;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;


@EventBusSubscriber(modid = DarkDoppelgangerMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        EntityRenderers.register(EntityRegistry.DARK_DOPPELGANGER.get(), DarkDoppelgangerRenderer::new);
        EntityRenderers.register(EntityRegistry.DARK_DOPPELGANGER_MINION.get(), DarkDoppelgangerMinionRenderer::new);
        EntityRenderers.register(EntityRegistry.PORTAL_JOIN_ENTITY.get(), PortalJoinRenderer::new);
        EntityRenderers.register(EntityRegistry.PORTAL_LEAVE_ENTITY.get(), PortalLeaveRenderer::new);
        EntityRenderers.register(EntityRegistry.ENDER_DAGGER_PROJECTILE.get(), EnderDaggerRenderer::new);
        EntityRenderers.register(EntityRegistry.ENDER_FIELD.get(), NoopRenderer::new);
    }
}
