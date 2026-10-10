package net.bandit.darkdoppelganger.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import mod.azure.azurelib.common.render.armor.AzArmorRenderer;
import mod.azure.azurelib.common.render.armor.AzArmorRendererRegistry;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;

/** Client-only bridge. Call only when AzureLib is installed. */
public final class AzureLibArmorCompat {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<Item> REPORTED_FAILURES = new HashSet<>();

    private AzureLibArmorCompat() {}

    public static boolean hasRenderer(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ArmorItem
                && AzArmorRendererRegistry.getOrNull(stack) != null;
    }

    public static void tryRender(PoseStack poseStack, AbstractSpellCastingMob entity,
                                 EquipmentSlot slot, ItemStack stack,
                                 HumanoidModel<?> innerModel, HumanoidModel<?> outerModel,
                                 int packedLight) {
        if (!hasRenderer(stack)) return;
        AzArmorRenderer renderer = AzArmorRendererRegistry.getOrNull(stack);
        if (renderer == null) return;

        HumanoidModel<?> baseModel = slot == EquipmentSlot.LEGS ? innerModel : outerModel;
        int color = stack.is(ItemTags.DYEABLE) ? DyedItemColor.getOrDefault(stack, -6265536) : -1;
        poseStack.pushPose();
        try {
            renderer.prepForRender(entity, stack, slot, baseModel);
            renderer.rendererPipeline().armorModel().renderToBuffer(
                    poseStack, null, packedLight, OverlayTexture.NO_OVERLAY, color);
        } catch (Exception e) {
            // Report each failing item once rather than flooding the log every frame.
            if (REPORTED_FAILURES.add(stack.getItem())) {
                LOGGER.warn("Could not render AzureLib armor {} in slot {} on a doppelganger",
                        stack.getItem(), slot, e);
            }
        } finally {
            poseStack.popPose();
        }
    }
}
