package net.bandit.darkdoppelganger;

import com.mojang.logging.LogUtils;
import net.bandit.darkdoppelganger.command.ModCommands;
import net.bandit.darkdoppelganger.client.renderer.ShadowAltarRenderer;
import net.bandit.darkdoppelganger.entity.DarkDoppelgangerEntity;
import net.bandit.darkdoppelganger.entity.DarkDoppelgangerMinionEntity;
import net.bandit.darkdoppelganger.entity.renderer.DarkDoppelgangerMinionRenderer;
import net.bandit.darkdoppelganger.entity.renderer.DarkDoppelgangerRenderer;
import net.bandit.darkdoppelganger.entity.renderer.PortalJoinRenderer;
import net.bandit.darkdoppelganger.entity.renderer.PortalLeaveRenderer;
import net.bandit.darkdoppelganger.registry.*;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import org.slf4j.Logger;

@Mod(DarkDoppelgangerMod.MOD_ID)
public class DarkDoppelgangerMod {
    public static final String MOD_ID = "darkdoppelganger";
    private static final Logger LOGGER = LogUtils.getLogger();

    public DarkDoppelgangerMod(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::onEntityAttributeCreation);

        SoundRegistry.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        EntityRegistry.register(modEventBus);
        SpellRegistry.register(modEventBus);
        ItemRegistry.register(modEventBus);
        TabRegistry.register(modEventBus);
        ComponentRegistry.init(modEventBus);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Common setup for Dark Doppelganger Mod");
    }

    private void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(EntityRegistry.DARK_DOPPELGANGER.get(), DarkDoppelgangerEntity.createAttributes().build());
        event.put(EntityRegistry.DARK_DOPPELGANGER_MINION.get(), DarkDoppelgangerMinionEntity.createAttributes().build());
    }

    @EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            EntityRenderers.register(EntityRegistry.DARK_DOPPELGANGER.get(), DarkDoppelgangerRenderer::new);
            EntityRenderers.register(EntityRegistry.DARK_DOPPELGANGER_MINION.get(), DarkDoppelgangerMinionRenderer::new);
            EntityRenderers.register(EntityRegistry.PORTAL_JOIN_ENTITY.get(), PortalJoinRenderer::new);
            EntityRenderers.register(EntityRegistry.PORTAL_LEAVE_ENTITY.get(), PortalLeaveRenderer::new);
            LOGGER.info("Client setup for Dark Doppelganger Mod");
        }

        @SubscribeEvent
        public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(ModBlockEntities.SHADOW_ALTAR.get(), ShadowAltarRenderer::new);
        }
    }
    @EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.GAME)
    public static class CommandRegistration {
        @SubscribeEvent
        public static void onRegisterCommands(RegisterCommandsEvent event){
            ModCommands.registerCommands(event.getDispatcher());
        }
    }
}
