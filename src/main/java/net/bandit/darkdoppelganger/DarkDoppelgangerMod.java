package net.bandit.darkdoppelganger;

import com.mojang.logging.LogUtils;
import net.bandit.darkdoppelganger.client.renderer.ShadowAltarRenderer;
import net.bandit.darkdoppelganger.command.ModCommands;
import net.bandit.darkdoppelganger.entity.DarkDoppelgangerEntity;
import net.bandit.darkdoppelganger.entity.DarkDoppelgangerMinionEntity;
import net.bandit.darkdoppelganger.entity.EntityRegistry;
import net.bandit.darkdoppelganger.network.CutscenePayloads;
import net.bandit.darkdoppelganger.registry.ItemRegistry;
import net.bandit.darkdoppelganger.registry.ModBlockEntities;
import net.bandit.darkdoppelganger.registry.ModBlocks;
import net.bandit.darkdoppelganger.registry.ModSounds;
import net.bandit.darkdoppelganger.registry.SpellRegistry;
import net.bandit.darkdoppelganger.registry.TabRegistry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(DarkDoppelgangerMod.MOD_ID)
public class DarkDoppelgangerMod {
    public static final String MOD_ID = "darkdoppelganger";
    private static final Logger LOGGER = LogUtils.getLogger();

    public DarkDoppelgangerMod() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::onEntityAttributeCreation);

        ModSounds.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        EntityRegistry.ENTITY_TYPES.register(modEventBus);
        SpellRegistry.register(modEventBus);
        ItemRegistry.ITEMS.register(modEventBus);
        TabRegistry.register(modEventBus);
        CutscenePayloads.register();
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Common setup for Dark Doppelganger Mod");
    }

    private void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(EntityRegistry.DARK_DOPPELGANGER.get(), DarkDoppelgangerEntity.createAttributes().build());
        event.put(EntityRegistry.DARK_DOPPELGANGER_MINION.get(), DarkDoppelgangerMinionEntity.createAttributes().build());
    }

    // Entity renderers are registered in client.ClientModEvents.
    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(ModBlockEntities.SHADOW_ALTAR.get(), ShadowAltarRenderer::new);
        }
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class CommandRegistration {
        @SubscribeEvent
        public static void onRegisterCommands(RegisterCommandsEvent event) {
            ModCommands.registerCommands(event.getDispatcher());
        }
    }
}
