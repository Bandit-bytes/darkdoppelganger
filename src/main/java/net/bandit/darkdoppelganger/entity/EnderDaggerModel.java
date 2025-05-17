package net.bandit.darkdoppelganger.entity;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.render.RenderHelper;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

import javax.annotation.Nullable;

public class EnderDaggerModel extends GeoModel<EnderDaggerEntity> {
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(IronsSpellbooks.MODID, "textures/entity/summoned_weapons/summoned_sword.png");
    public static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(IronsSpellbooks.MODID, "geo/summoned_sword.geo.json");

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
        return AbstractSpellCastingMob.animationInstantCast;
    }

    @Override
    public @Nullable RenderType getRenderType(EnderDaggerEntity animatable, ResourceLocation texture) {
        return RenderHelper.CustomerRenderType.magic(TEXTURE);
    }
}
