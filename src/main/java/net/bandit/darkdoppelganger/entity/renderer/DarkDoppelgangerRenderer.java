package net.bandit.darkdoppelganger.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMobRenderer;
import net.bandit.darkdoppelganger.entity.DarkDoppelgangerEntity;
import net.bandit.darkdoppelganger.entity.model.DarkDoppelgangerEntityModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;

public class DarkDoppelgangerRenderer extends AbstractSpellCastingMobRenderer {

    public DarkDoppelgangerRenderer(EntityRendererProvider.Context context) {
        super(context, new DarkDoppelgangerEntityModel());
        this.shadowRadius = 0.3f;
    }

    @Override
    public void render(AbstractSpellCastingMob entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        shadowStrength = 1;
        shadowRadius = .3f;
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, Math.clamp(packedLight + 100, 0, LightTexture.FULL_BLOCK));
    }

    @Override
    public int getPackedOverlay(AbstractSpellCastingMob animatable, float u, float partialTick) {
        //disable hurt/death red flashing
        return OverlayTexture.NO_OVERLAY;
    }

    @Override
    protected float getDeathMaxRotation(AbstractSpellCastingMob animatable) {
        return 0;
    }
}
