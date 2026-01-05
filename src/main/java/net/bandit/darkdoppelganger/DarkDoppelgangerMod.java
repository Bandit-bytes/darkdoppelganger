package net.bandit.darkdoppelganger;

import com.mojang.logging.LogUtils;
import net.bandit.darkdoppelganger.command.ModCommands;
import net.bandit.darkdoppelganger.entity.DarkDoppelgangerMinionEntity;
import net.bandit.darkdoppelganger.entity.PortalJoinEntity;
import net.bandit.darkdoppelganger.entity.PortalLeaveEntity;
import net.bandit.darkdoppelganger.entity.renderer.DarkDoppelgangerMinionRenderer;
import net.bandit.darkdoppelganger.entity.renderer.PortalJoinRenderer;
import net.bandit.darkdoppelganger.entity.renderer.PortalLeaveRenderer;
import net.bandit.darkdoppelganger.registry.ItemRegistry;
import net.bandit.darkdoppelganger.registry.ModSounds;
import net.bandit.darkdoppelganger.registry.SpellRegistry;
import net.bandit.darkdoppelganger.registry.TabRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;
import net.bandit.darkdoppelganger.entity.DarkDoppelgangerEntity;
import net.bandit.darkdoppelganger.entity.renderer.DarkDoppelgangerRenderer;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;


@Mod(DarkDoppelgangerMod.MOD_ID)
public class DarkDoppelgangerMod {

    public static final String MOD_ID = "darkdoppelganger";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MOD_ID);

    public static final RegistryObject<EntityType<DarkDoppelgangerEntity>> DARK_DOPPELGANGER = ENTITY_TYPES.register("dark_doppelganger",
            () -> EntityType.Builder.of(DarkDoppelgangerEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F)
                    .build(MOD_ID + ":dark_doppelganger")
    );
    public static final RegistryObject<EntityType<DarkDoppelgangerMinionEntity>> DARK_DOPPELGANGER_MINION =
            ENTITY_TYPES.register("dark_doppelganger_minion",
                    () -> EntityType.Builder.<DarkDoppelgangerMinionEntity>of(DarkDoppelgangerMinionEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.95f)
                            .build(new ResourceLocation(MOD_ID, "dark_doppelganger_minion").toString()));


    public static final RegistryObject<EntityType<PortalJoinEntity>> PORTAL_JOIN_ENTITY =
            ENTITY_TYPES.register("portal_join_entity", () -> EntityType.Builder.<PortalJoinEntity>of(PortalJoinEntity::new, MobCategory.MISC)
                    .sized(.1f, 3f)
                    .clientTrackingRange(64)
                    .build(new ResourceLocation(DarkDoppelgangerMod.MOD_ID, "portal_join_entity").toString()));

    public static final RegistryObject<EntityType<PortalLeaveEntity>> PORTAL_LEAVE_ENTITY =
            ENTITY_TYPES.register("portal_leave_entity", () -> EntityType.Builder.<PortalLeaveEntity>of(PortalLeaveEntity::new, MobCategory.MISC)
                    .sized(3f, .1f)
                    .clientTrackingRange(64)
                    .build(new ResourceLocation(DarkDoppelgangerMod.MOD_ID, "portal_leave_entity").toString()));


    public DarkDoppelgangerMod() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.COMMON_CONFIG);
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::onEntityAttributeCreation);
        modEventBus.addListener(this::addCreative);


        ItemRegistry.ITEMS.register(modEventBus);
        TabRegistry.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        SpellRegistry.register(modEventBus);
        ModSounds.register(modEventBus);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
    }


    @SubscribeEvent
    public void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(DARK_DOPPELGANGER.get(), DarkDoppelgangerEntity.createAttributes().build());
        event.put(DARK_DOPPELGANGER_MINION.get(), DarkDoppelgangerMinionEntity.createAttributes().build());
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        // Add items to creative tab
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            EntityRenderers.register(DARK_DOPPELGANGER.get(), DarkDoppelgangerRenderer::new);
            EntityRenderers.register(DARK_DOPPELGANGER_MINION.get(), DarkDoppelgangerMinionRenderer::new);
            EntityRenderers.register(PORTAL_JOIN_ENTITY.get(), PortalJoinRenderer::new);
            EntityRenderers.register(PORTAL_LEAVE_ENTITY.get(), PortalLeaveRenderer::new);
        }
    }
    @Mod.EventBusSubscriber(modid = DarkDoppelgangerMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class CommandRegistration {
        @SubscribeEvent
        public static void onRegisterCommands(net.minecraftforge.event.RegisterCommandsEvent event) {
            ModCommands.registerCommands(event.getDispatcher());
        }
    }

}

