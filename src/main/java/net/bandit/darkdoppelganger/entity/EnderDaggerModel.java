package net.bandit.darkdoppelganger.entity;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class EnderDaggerModel extends GeoModel<EnderDaggerEntity> {
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(IronsSpellbooks.MODID, "textures/entity/summoned_weapons/summoned_sword.png");
    public static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(IronsSpellbooks.MODID, "geo/summoned_sword.geo.json");
    public static final ResourceLocation ANIMATIONS = ResourceLocation.fromNamespaceAndPath(IronsSpellbooks.MODID, "animations/summoned_weapon_animations.json");

    @Override
    public ResourceLocation getModelResource(EnderDaggerEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(EnderDaggerEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(EnderDaggerEntity animatable) {
        return ANIMATIONS;
    }
}
