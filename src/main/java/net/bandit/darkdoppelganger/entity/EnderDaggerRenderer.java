package net.bandit.darkdoppelganger.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.redspace.ironsspellbooks.render.RenderHelper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.Color;

import javax.annotation.Nullable;

public class EnderDaggerRenderer extends GeoEntityRenderer<EnderDaggerEntity> {
    public EnderDaggerRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new EnderDaggerModel());
    }

    @Override
    public void preRender(PoseStack poseStack, EnderDaggerEntity entity, BakedGeoModel model, @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, boolean isReRender, float partialTicks, int packedLight, int packedOverlay, int colour) {
        super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender, partialTicks, packedLight, packedOverlay, colour);
        poseStack.translate(0, entity.getBbHeight() * .5f, 0);
        poseStack.mulPose(Axis.XP.rotationDegrees(-90));
    }

    @Override
    public @Nullable RenderType getRenderType(EnderDaggerEntity animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderHelper.CustomerRenderType.magic(texture);
    }

    @Override
    public Color getRenderColor(EnderDaggerEntity animatable, float partialTick, int packedLight) {
        return Color.MAGENTA;
    }
}
