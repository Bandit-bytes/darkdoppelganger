package net.bandit.darkdoppelganger.entity;

import net.bandit.darkdoppelganger.DarkDoppelgangerMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class EntityRegistry {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, DarkDoppelgangerMod.MOD_ID);


    public static final RegistryObject<EntityType<DarkDoppelgangerEntity>> DARK_DOPPELGANGER =
            ENTITY_TYPES.register("dark_doppelganger",
                    () -> EntityType.Builder.of(DarkDoppelgangerEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.8F)
                            .setTrackingRange(80)
                            .setUpdateInterval(3)
                            .setShouldReceiveVelocityUpdates(true)
                            .build(DarkDoppelgangerMod.MOD_ID + ":dark_doppelganger"));

    public static final RegistryObject<EntityType<DarkDoppelgangerMinionEntity>> DARK_DOPPELGANGER_MINION =
            ENTITY_TYPES.register("dark_doppelganger_minion",
                    () -> EntityType.Builder.of(DarkDoppelgangerMinionEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.8F)
                            .setTrackingRange(80)
                            .setUpdateInterval(3)
                            .clientTrackingRange(80)
                            .setShouldReceiveVelocityUpdates(true)
                            .build(DarkDoppelgangerMod.MOD_ID + ":dark_doppelganger_minion"));


    public static final RegistryObject<EntityType<PortalJoinEntity>> PORTAL_JOIN_ENTITY =
            ENTITY_TYPES.register("portal_join_entity", () -> EntityType.Builder.<PortalJoinEntity>of(PortalJoinEntity::new, MobCategory.MISC)
                    .sized(.1f, 3f)
                    .clientTrackingRange(64)
                    .build(ResourceLocation.fromNamespaceAndPath(DarkDoppelgangerMod.MOD_ID, "portal_join_entity").toString()));

    public static final RegistryObject<EntityType<PortalLeaveEntity>> PORTAL_LEAVE_ENTITY =
            ENTITY_TYPES.register("portal_leave_entity", () -> EntityType.Builder.<PortalLeaveEntity>of(PortalLeaveEntity::new, MobCategory.MISC)
                    .sized(3f, .1f)
                    .clientTrackingRange(64)
                    .build(ResourceLocation.fromNamespaceAndPath(DarkDoppelgangerMod.MOD_ID, "portal_leave_entity").toString()));

}
