package net.bandit.darkdoppelganger.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.util.DefaultBipedBoneIdents;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.ClientHooks;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.ItemArmorGeoLayer;

/** Keeps vanilla/GeckoLib armor on bones; AzureLib armor is drawn once as a whole model. */
public final class DoppelgangerArmorGeoLayer extends ItemArmorGeoLayer<AbstractSpellCastingMob> {
    public DoppelgangerArmorGeoLayer(GeoRenderer<AbstractSpellCastingMob> renderer) {
        super(renderer);
    }

    @Override
    @Nullable
    protected ItemStack getArmorItemForBone(GeoBone bone, AbstractSpellCastingMob entity) {
        return switch (bone.getName()) {
            case DefaultBipedBoneIdents.LEFT_FOOT_ARMOR_BONE_IDENT,
                 DefaultBipedBoneIdents.RIGHT_FOOT_ARMOR_BONE_IDENT,
                 DefaultBipedBoneIdents.LEFT_FOOT_ARMOR_BONE_2_IDENT,
                 DefaultBipedBoneIdents.RIGHT_FOOT_ARMOR_BONE_2_IDENT -> bootsStack;
            case DefaultBipedBoneIdents.LEFT_LEG_ARMOR_BONE_IDENT,
                 DefaultBipedBoneIdents.RIGHT_LEG_ARMOR_BONE_IDENT,
                 DefaultBipedBoneIdents.LEFT_LEG_ARMOR_BONE_2_IDENT,
                 DefaultBipedBoneIdents.RIGHT_LEG_ARMOR_BONE_2_IDENT -> leggingsStack;
            case DefaultBipedBoneIdents.BODY_ARMOR_BONE_IDENT,
                 DefaultBipedBoneIdents.RIGHT_ARM_ARMOR_BONE_IDENT,
                 DefaultBipedBoneIdents.LEFT_ARM_ARMOR_BONE_IDENT -> chestplateStack;
            case DefaultBipedBoneIdents.HEAD_ARMOR_BONE_IDENT -> helmetStack;
            default -> null;
        };
    }

    @Override
    protected EquipmentSlot getEquipmentSlotForBone(GeoBone bone, ItemStack stack,
                                                      AbstractSpellCastingMob entity) {
        return switch (bone.getName()) {
            case DefaultBipedBoneIdents.LEFT_FOOT_ARMOR_BONE_IDENT,
                 DefaultBipedBoneIdents.RIGHT_FOOT_ARMOR_BONE_IDENT,
                 DefaultBipedBoneIdents.LEFT_FOOT_ARMOR_BONE_2_IDENT,
                 DefaultBipedBoneIdents.RIGHT_FOOT_ARMOR_BONE_2_IDENT -> EquipmentSlot.FEET;
            case DefaultBipedBoneIdents.LEFT_LEG_ARMOR_BONE_IDENT,
                 DefaultBipedBoneIdents.RIGHT_LEG_ARMOR_BONE_IDENT,
                 DefaultBipedBoneIdents.LEFT_LEG_ARMOR_BONE_2_IDENT,
                 DefaultBipedBoneIdents.RIGHT_LEG_ARMOR_BONE_2_IDENT -> EquipmentSlot.LEGS;
            case DefaultBipedBoneIdents.BODY_ARMOR_BONE_IDENT,
                 DefaultBipedBoneIdents.RIGHT_ARM_ARMOR_BONE_IDENT,
                 DefaultBipedBoneIdents.LEFT_ARM_ARMOR_BONE_IDENT -> EquipmentSlot.CHEST;
            case DefaultBipedBoneIdents.HEAD_ARMOR_BONE_IDENT -> EquipmentSlot.HEAD;
            default -> super.getEquipmentSlotForBone(bone, stack, entity);
        };
    }

    @Override
    protected ModelPart getModelPartForBone(GeoBone bone, EquipmentSlot slot, ItemStack stack,
                                            AbstractSpellCastingMob entity, HumanoidModel<?> baseModel) {
        return switch (bone.getName()) {
            case DefaultBipedBoneIdents.LEFT_FOOT_ARMOR_BONE_IDENT,
                 DefaultBipedBoneIdents.LEFT_FOOT_ARMOR_BONE_2_IDENT,
                 DefaultBipedBoneIdents.LEFT_LEG_ARMOR_BONE_IDENT,
                 DefaultBipedBoneIdents.LEFT_LEG_ARMOR_BONE_2_IDENT -> baseModel.leftLeg;
            case DefaultBipedBoneIdents.RIGHT_FOOT_ARMOR_BONE_IDENT,
                 DefaultBipedBoneIdents.RIGHT_FOOT_ARMOR_BONE_2_IDENT,
                 DefaultBipedBoneIdents.RIGHT_LEG_ARMOR_BONE_IDENT,
                 DefaultBipedBoneIdents.RIGHT_LEG_ARMOR_BONE_2_IDENT -> baseModel.rightLeg;
            case DefaultBipedBoneIdents.RIGHT_ARM_ARMOR_BONE_IDENT -> baseModel.rightArm;
            case DefaultBipedBoneIdents.LEFT_ARM_ARMOR_BONE_IDENT -> baseModel.leftArm;
            case DefaultBipedBoneIdents.BODY_ARMOR_BONE_IDENT -> baseModel.body;
            case DefaultBipedBoneIdents.HEAD_ARMOR_BONE_IDENT -> baseModel.head;
            default -> super.getModelPartForBone(bone, slot, stack, entity, baseModel);
        };
    }

    @Override
    public void renderForBone(PoseStack poseStack, AbstractSpellCastingMob entity, GeoBone bone,
                              RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                              float partialTick, int packedLight, int packedOverlay) {
        ItemStack stack = getArmorItemForBone(bone, entity);
        if (AzureCompatibleDoppelgangerRenderer.AZURELIB_LOADED && stack != null
                && AzureLibArmorCompat.hasRenderer(stack)) {
            return;
        }
        super.renderForBone(poseStack, entity, bone, renderType, bufferSource, buffer,
                partialTick, packedLight, packedOverlay);
    }

    @Override
    protected VertexConsumer getVanillaArmorBuffer(MultiBufferSource bufferSource,
            AbstractSpellCastingMob entity, ItemStack stack, EquipmentSlot slot, GeoBone bone,
            @Nullable ArmorMaterial.Layer layer, int packedLight, int packedOverlay, boolean forGlint) {
        if (forGlint || layer == null) {
            return super.getVanillaArmorBuffer(bufferSource, entity, stack, slot, bone,
                    layer, packedLight, packedOverlay, forGlint);
        }
        ResourceLocation texture = ClientHooks.getArmorTexture(
                entity, stack, layer, slot == EquipmentSlot.LEGS, slot);
        return bufferSource.getBuffer(RenderType.armorCutoutNoCull(texture));
    }
}
