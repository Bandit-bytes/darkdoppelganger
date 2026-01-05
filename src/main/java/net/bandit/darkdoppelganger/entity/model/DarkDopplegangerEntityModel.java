package net.bandit.darkdoppelganger.entity.model;

import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMobModel;
import net.bandit.darkdoppelganger.DarkDoppelgangerMod;
import net.minecraft.resources.ResourceLocation;

public class DarkDopplegangerEntityModel extends AbstractSpellCastingMobModel {
    public static final ResourceLocation ANIMATIONS = ResourceLocation.fromNamespaceAndPath(DarkDoppelgangerMod.MOD_ID, "animations/doppel_casting_animations.json");
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(DarkDoppelgangerMod.MOD_ID, "textures/entity/dark_doppelganger.png");

    @Override
    public ResourceLocation getTextureResource(AbstractSpellCastingMob object) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(AbstractSpellCastingMob animatable) {
        return ANIMATIONS;
    }
}
