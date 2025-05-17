package net.bandit.darkdoppelganger.util;

import net.bandit.darkdoppelganger.DarkDoppelgangerMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

public class ModTags {
    public static final TagKey<EntityType<?>> CLONES = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(DarkDoppelgangerMod.MOD_ID, "dark_doppelganger_clone"));
}
