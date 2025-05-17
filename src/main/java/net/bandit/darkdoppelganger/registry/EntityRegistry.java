package net.bandit.darkdoppelganger.registry;

import net.bandit.darkdoppelganger.DarkDoppelgangerMod;
import net.bandit.darkdoppelganger.entity.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class EntityRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, DarkDoppelgangerMod.MOD_ID);


    public static final DeferredHolder<EntityType<?>, EntityType<DarkDoppelgangerEntity>> DARK_DOPPELGANGER =
            ENTITY_TYPES.register("dark_doppelganger",
                    () -> EntityType.Builder.of(DarkDoppelgangerEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.8F)
                            .setTrackingRange(80)
                            .setUpdateInterval(3)
                            .clientTrackingRange(80)
                            .setShouldReceiveVelocityUpdates(true)
                            .build(ResourceLocation.fromNamespaceAndPath(DarkDoppelgangerMod.MOD_ID, "dark_doppelganger").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<PortalJoinEntity>> PORTAL_JOIN_ENTITY =
            ENTITY_TYPES.register("portal_join_entity", () -> EntityType.Builder.of(PortalJoinEntity::new, MobCategory.MISC)
                    .sized(.1f, 3f)
                    .clientTrackingRange(64)
                    .build(ResourceLocation.fromNamespaceAndPath(DarkDoppelgangerMod.MOD_ID, "portal_join_entity").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<PortalLeaveEntity>> PORTAL_LEAVE_ENTITY =
            ENTITY_TYPES.register("portal_leave_entity", () -> EntityType.Builder.of(PortalLeaveEntity::new, MobCategory.MISC)
                    .sized(3f, .1f)
                    .clientTrackingRange(64)
                    .build(ResourceLocation.fromNamespaceAndPath(DarkDoppelgangerMod.MOD_ID, "portal_leave_entity").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<EnderDaggerEntity>> ENDER_DAGGER_PROJECTILE =
            ENTITY_TYPES.register("ender_dagger", () -> EntityType.Builder.<EnderDaggerEntity>of(EnderDaggerEntity::new, MobCategory.MISC)
                    .sized(.5f, .5f)
                    .clientTrackingRange(64)
                    .build(ResourceLocation.fromNamespaceAndPath(DarkDoppelgangerMod.MOD_ID, "ender_dagger").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<EnderField>> ENDER_FIELD =
            ENTITY_TYPES.register("ender_field", () -> EntityType.Builder.<EnderField>of(EnderField::new, MobCategory.MISC)
                    .sized(4f, 1.2f)
                    .clientTrackingRange(64)
                    .build(ResourceLocation.fromNamespaceAndPath(DarkDoppelgangerMod.MOD_ID, "ender_field").toString()));

    // Method to register sound events
    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
