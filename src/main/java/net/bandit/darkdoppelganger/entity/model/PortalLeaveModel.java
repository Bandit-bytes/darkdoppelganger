package net.bandit.darkdoppelganger.entity.model;

import net.bandit.darkdoppelganger.DarkDoppelgangerMod;
import net.bandit.darkdoppelganger.entity.PortalLeaveEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class PortalLeaveModel extends GeoModel<PortalLeaveEntity> {
    private static final ResourceLocation TEXTURE =  ResourceLocation.fromNamespaceAndPath(DarkDoppelgangerMod.MOD_ID, "textures/entity/portal.png");
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(DarkDoppelgangerMod.MOD_ID, "geo/portal.geo.json");
    public static final ResourceLocation ANIMS =  ResourceLocation.fromNamespaceAndPath(DarkDoppelgangerMod.MOD_ID, "animations/model_portal_leave.animation.json");


    public PortalLeaveModel() {
    }

    @Override
    public ResourceLocation getTextureResource(PortalLeaveEntity object) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getModelResource(PortalLeaveEntity object) {
        return MODEL;
    }

    @Override
    public ResourceLocation getAnimationResource(PortalLeaveEntity animatable) {
        return ANIMS;
    }
}
