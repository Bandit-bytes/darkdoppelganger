package net.bandit.darkdoppelganger.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.bandit.darkdoppelganger.DarkDoppelgangerMod;
import net.bandit.darkdoppelganger.entity.PortalJoinEntity;
import net.bandit.darkdoppelganger.entity.PortalLeaveEntity;
import net.bandit.darkdoppelganger.entity.model.PortalJoinModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class PortalJoinRenderer extends GeoEntityRenderer<PortalJoinEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(DarkDoppelgangerMod.MOD_ID, "textures/entity/portal.png");

    public PortalJoinRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new PortalJoinModel());
        this.shadowRadius = 0;
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override
    public void render(PortalJoinEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(PortalJoinEntity animatable) {
        return TEXTURE;
    }
}
