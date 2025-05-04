package net.bandit.darkdoppelganger.items;

import net.bandit.darkdoppelganger.curios.CurioBaseItem;
import io.redspace.ironsspellbooks.item.weapons.AttributeContainer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import top.theillusivec4.curios.api.SlotContext;

import java.util.List;

import static io.redspace.ironsspellbooks.api.registry.AttributeRegistry.COOLDOWN_REDUCTION;

public class DoppelgangerRingItem extends CurioBaseItem {
    private static final int BASE_COOLDOWN = 100;
    private static final double HEALTH_BONUS = 20.0;
    private static final double COOLDOWN_REDUCTION_PERCENT = 0.2;

    public DoppelgangerRingItem(Properties properties) {
        super(properties);

        // Configure the ring's attributes
        withAttributes(
                new AttributeContainer(Attributes.MAX_HEALTH, HEALTH_BONUS, AttributeModifier.Operation.ADD_VALUE),
                new AttributeContainer(COOLDOWN_REDUCTION, COOLDOWN_REDUCTION_PERCENT, AttributeModifier.Operation.ADD_VALUE)
        );
    }

    /**
     * Handles the logic for applying cooldown on item usage.
     *
     * @param player The player using the item.
     * @return True if the cooldown was successfully applied, false otherwise.
     */
    public boolean tryProcCooldown(Player player) {
        if (player.getCooldowns().isOnCooldown(this)) {
            return false;
        } else {
            player.getCooldowns().addCooldown(this, BASE_COOLDOWN);
            return true;
        }
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        LivingEntity entity = slotContext.entity();
        if (entity instanceof Player player) {
            if (player.getHealth() < player.getMaxHealth() && player.tickCount % 20 == 0) { // Heal once per second
                player.heal(0.5F); // Heal 0.5 health (quarter heart)
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("tooltip.doppelganger.passive_ability", BASE_COOLDOWN / 20)
                .withStyle(ChatFormatting.GREEN));
        tooltipComponents.add(Component.translatable("tooltip.doppelganger.health_bonus", HEALTH_BONUS)
                .withStyle(ChatFormatting.GOLD));
        tooltipComponents.add(Component.translatable("tooltip.doppelganger.cooldown_reduction", "20%")
                .withStyle(ChatFormatting.AQUA));
    }
}
