package net.bandit.darkdoppelganger.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.bandit.darkdoppelganger.block.entity.ShadowAltarBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class ShadowAltarRenderer implements BlockEntityRenderer<ShadowAltarBlockEntity> {
    private final ItemRenderer itemRenderer;

    public ShadowAltarRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(
            ShadowAltarBlockEntity altar,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay
    ) {
        ItemStack orb = altar.getOrbForRender();
        if (orb.isEmpty()) {
            return;
        }

        long gameTime = altar.getLevel() == null ? 0L : altar.getLevel().getGameTime();
        float age = gameTime + partialTick;
        double bob = Math.sin(age * 0.10F) * 0.08;

        poseStack.pushPose();
        poseStack.translate(0.5, 1.55 + bob, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(age * 3.5F));
        poseStack.scale(0.75F, 0.75F, 0.75F);
        itemRenderer.renderStatic(
                orb,
                ItemDisplayContext.GROUND,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                altar.getLevel(),
                0
        );
        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(ShadowAltarBlockEntity altar) {
        return true;
    }
}
