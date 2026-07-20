package net.bandit.darkdoppelganger.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class ShadowOrbItem extends Item {
    public ShadowOrbItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltipComponents,
            TooltipFlag tooltipFlag
    ) {
        tooltipComponents.add(
                Component.translatable("orb.tooltip1").withStyle(ChatFormatting.DARK_PURPLE)
        );
        tooltipComponents.add(
                Component.translatable("orb.tooltip2").withStyle(ChatFormatting.GRAY)
        );
    }
}
