package net.bandit.darkdoppelganger.registry;

import net.bandit.darkdoppelganger.DarkDoppelgangerMod;
import net.bandit.darkdoppelganger.entity.DarkDoppelgangerEntity;
import net.bandit.darkdoppelganger.entity.PortalJoinEntity;
import net.bandit.darkdoppelganger.entity.PortalLeaveEntity;
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

    // Method to register sound events
    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
