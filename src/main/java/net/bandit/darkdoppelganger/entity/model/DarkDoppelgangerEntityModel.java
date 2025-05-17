package net.bandit.darkdoppelganger.entity.model;

import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMobModel;
import io.redspace.ironsspellbooks.registries.ParticleRegistry;
import io.redspace.ironsspellbooks.util.DefaultBipedBoneIdents;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.bandit.darkdoppelganger.DarkDoppelgangerMod;
import net.bandit.darkdoppelganger.entity.DarkDoppelgangerEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.PartNames;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.WalkAnimationState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;

public class DarkDoppelgangerEntityModel extends AbstractSpellCastingMobModel {
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(DarkDoppelgangerMod.MOD_ID, "textures/entity/dark_doppelganger.png");

    private static final float tilt = 15 * Mth.DEG_TO_RAD;
    private static final Vector3f forward = new Vector3f(0, 0, Mth.sin(tilt) * -12);

    @Override
    public ResourceLocation getTextureResource(AbstractSpellCastingMob object) {
        return TEXTURE;
    }

    @Override
    public void setCustomAnimations(AbstractSpellCastingMob entity, long instanceId, AnimationState<AbstractSpellCastingMob> animationState) {
        if (Minecraft.getInstance().isPaused()) {
            return;
        }
        if (entity instanceof DarkDoppelgangerEntity darkDoppelgangerEntity) {
            handleParticles(darkDoppelgangerEntity);
            float partialTick = animationState.getPartialTick();
            Vector2f limbSwing = getLimbSwing(entity, entity.walkAnimation, partialTick);
            if (entity.isAnimating()) {
                darkDoppelgangerEntity.isAnimatingDampener = Mth.lerp(.15f * partialTick, darkDoppelgangerEntity.isAnimatingDampener, 0);
            } else {
                darkDoppelgangerEntity.isAnimatingDampener = Mth.lerp(.05f * partialTick, darkDoppelgangerEntity.isAnimatingDampener, 1);
            }
            GeoBone rightArm = this.getAnimationProcessor().getBone(PartNames.RIGHT_ARM);
            GeoBone rightHand = this.getAnimationProcessor().getBone(DefaultBipedBoneIdents.RIGHT_HAND_BONE_IDENT);
            Vector3f armPose = new Vector3f(-30, -30, 10);
            armPose.mul(Mth.DEG_TO_RAD * darkDoppelgangerEntity.isAnimatingDampener);
            transformStack.pushRotation(rightArm, armPose);

            Vector3f scythePos = new Vector3f(-5, 0, -48);
            scythePos.mul(Mth.DEG_TO_RAD * darkDoppelgangerEntity.isAnimatingDampener);
            transformStack.pushRotation(rightHand, scythePos);

            if (!entity.isAnimating()) {
                float walkDampener = (Mth.cos(limbSwing.y() * 0.6662F + (float) Math.PI) * 2.0F * limbSwing.x() * 0.5F) * -.75f;
                transformStack.pushRotation(rightArm, walkDampener, 0, 0);
            }
        }
        super.setCustomAnimations(entity, instanceId, animationState);
    }

    public void handleParticles(DarkDoppelgangerEntity entity) {
        GeoBone body = this.getAnimationProcessor().getBone("body");
        GeoBone offhand = this.getAnimationProcessor().getBone(DefaultBipedBoneIdents.LEFT_HAND_BONE_IDENT);

        if (entity.clientDaggerParticles) {
            if (offhand.isTrackingMatrices()) {
                Vector3d pos = offhand.getWorldPosition();
                for (int i = 0; i < 15; i++) {
                    Vec3 random = Utils.getRandomVec3(0.25);
                    entity.level().addParticle(ParticleHelper.UNSTABLE_ENDER, pos.x, pos.y, pos.z, random.x, random.y, random.z);
                }
                entity.clientDaggerParticles = false;
            } else {
                offhand.setTrackingMatrices(true);
            }
        }
        body.setTrackingMatrices(false);
    }

    @Override
    protected Vector2f getLimbSwing(AbstractSpellCastingMob entity, WalkAnimationState walkAnimationState, float partialTick) {
        Vector2f swing = super.getLimbSwing(entity, walkAnimationState, partialTick);
        swing.mul(0.6f, 1f);
        return swing;
    }
}
