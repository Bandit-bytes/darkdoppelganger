package net.bandit.darkdoppelganger.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ShadowOrbItem extends Item {

    public ShadowOrbItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("orb.tooltip1").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.translatable("orb.tooltip2").withStyle(ChatFormatting.GRAY));
    }
}
