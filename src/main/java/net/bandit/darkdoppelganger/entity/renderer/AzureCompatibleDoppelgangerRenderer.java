package net.bandit.darkdoppelganger.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMobModel;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMobRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.layer.ItemArmorGeoLayer;

/** Shared client renderer for the boss and its minions. */
public abstract class AzureCompatibleDoppelgangerRenderer extends AbstractSpellCastingMobRenderer {
    static final boolean AZURELIB_LOADED = ModList.get().isLoaded("azurelib");
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    private final HumanoidModel<LivingEntity> innerArmorModel;
    private final HumanoidModel<LivingEntity> outerArmorModel;

    protected AzureCompatibleDoppelgangerRenderer(EntityRendererProvider.Context context,
                                                   AbstractSpellCastingMobModel model) {
        super(context, model);
        // Replace only armor. Keep the inherited held-item, cape and spell-effect behavior.
        getRenderLayers().removeIf(layer -> layer instanceof ItemArmorGeoLayer<?>);
        addRenderLayer(new DoppelgangerArmorGeoLayer(this));
        innerArmorModel = new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR));
        outerArmorModel = new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR));
    }

    @Override
    public void actuallyRender(PoseStack poseStack, AbstractSpellCastingMob entity, BakedGeoModel model,
                               @Nullable RenderType renderType, MultiBufferSource bufferSource,
                               @Nullable VertexConsumer buffer, boolean isReRender, float partialTick,
                               int packedLight, int packedOverlay, int colour) {
        super.actuallyRender(poseStack, entity, model, renderType, bufferSource, buffer,
                isReRender, partialTick, packedLight, packedOverlay, colour);
        // Effect/hologram re-renders must not draw an extra opaque armor set.
        if (!isReRender && AZURELIB_LOADED && buffer != null) {
            renderAzureArmor(poseStack, entity, model, partialTick, packedLight);
        }
    }

    private void renderAzureArmor(PoseStack poseStack, AbstractSpellCastingMob entity,
                                   BakedGeoModel model, float partialTick, int packedLight) {
        boolean hasAzureArmor = false;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (AzureLibArmorCompat.hasRenderer(entity.getItemBySlot(slot))) {
                hasAzureArmor = true;
                break;
            }
        }
        if (!hasAzureArmor) return;

        poseStack.pushPose();
        try {
            // GeoEntityRenderer has popped its transform frame; reconstruct the same frame.
            float bodyYaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
            if (entity.isPassenger() && entity.getVehicle() instanceof LivingEntity vehicle) {
                bodyYaw = Mth.rotLerp(partialTick, vehicle.yBodyRotO, vehicle.yBodyRot);
                float headYaw = Mth.rotLerp(partialTick, entity.yHeadRotO, entity.yHeadRot);
                float clamped = Mth.clamp(Mth.wrapDegrees(headYaw - bodyYaw), -85, 85);
                bodyYaw = headYaw - clamped;
                if (clamped * clamped > 2500f) bodyYaw += clamped * 0.2f;
            }
            if (entity.getPose() == Pose.SLEEPING) {
                Direction bedDirection = entity.getBedOrientation();
                if (bedDirection != null) {
                    float offset = entity.getEyeHeight(Pose.STANDING) - 0.1f;
                    poseStack.translate(-bedDirection.getStepX() * offset, 0,
                            -bedDirection.getStepZ() * offset);
                }
            }
            float scale = entity.getScale();
            poseStack.scale(scale, scale, scale);
            applyRotations(entity, poseStack, entity.tickCount + partialTick, bodyYaw, partialTick, scale);
            poseStack.translate(0, 0.01f, 0);
            poseBaseFromGeo(outerArmorModel, model, entity);
            poseBaseFromGeo(innerArmorModel, model, entity);
            poseStack.scale(-1, -1, 1);
            poseStack.translate(0, -1.501f, 0);
            for (EquipmentSlot slot : ARMOR_SLOTS) {
                AzureLibArmorCompat.tryRender(poseStack, entity, slot, entity.getItemBySlot(slot),
                        innerArmorModel, outerArmorModel, packedLight);
            }
        } finally {
            poseStack.popPose();
        }
    }

    private static void poseBaseFromGeo(HumanoidModel<?> base, BakedGeoModel model,
                                         AbstractSpellCastingMob entity) {
        base.young = entity.isBaby();
        base.riding = entity.isPassenger();
        // Crouching is already in the animated bones; do not apply a second vanilla crouch.
        base.crouching = false;
        base.leftArmPose = HumanoidModel.ArmPose.EMPTY;
        base.rightArmPose = HumanoidModel.ArmPose.EMPTY;
        copyBone(model, "head", base.head, 0, 0, 0);
        copyBone(model, "torso", base.body, 0, 0, 0);
        copyBone(model, "left_arm", base.leftArm, 5, 2, 0);
        copyBone(model, "right_arm", base.rightArm, -5, 2, 0);
        copyBone(model, "left_leg", base.leftLeg, 1.9f, 12, 0);
        copyBone(model, "right_leg", base.rightLeg, -1.9f, 12, 0);
        base.hat.copyFrom(base.head);
    }

    private static void copyBone(BakedGeoModel model, String name, ModelPart part,
                                 float defaultX, float defaultY, float defaultZ) {
        // Always reset: a missing bone must not leave the previous entity's pose behind.
        part.resetPose();
        part.setPos(defaultX, defaultY, defaultZ);
        part.visible = true;
        model.getBone(name).ifPresent(bone -> {
            part.xRot = -bone.getRotX();
            part.yRot = -bone.getRotY();
            part.zRot = bone.getRotZ();
            part.x += bone.getPosX();
            part.y -= bone.getPosY();
            part.z += bone.getPosZ();
            part.xScale = bone.getScaleX();
            part.yScale = bone.getScaleY();
            part.zScale = bone.getScaleZ();
        });
    }
}
