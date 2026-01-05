package net.bandit.darkdoppelganger.item;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SummonScrollItem extends Item {

    public SummonScrollItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        Player player = context.getPlayer();
        ItemStack itemStack = context.getItemInHand();

        if (!world.isClientSide && player != null) {
            player.sendSystemMessage(Component.literal("Item is depreciated."));

            ServerLevel serverWorld = (ServerLevel) world;
            serverWorld.getServer().submitAsync(() -> {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();

                };
            });
            if (!player.isCreative()) {
                itemStack.shrink(1);
            }

            return InteractionResult.SUCCESS;
        }
        if (world.isClientSide) {
            triggerTotemAnimation(player, itemStack);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    private void triggerTotemAnimation(Player player, ItemStack itemStack) {
        if (player.level().isClientSide()) {

            player.playSound(SoundEvents.TOTEM_USE, 1.0F, 1.0F);


            Minecraft.getInstance().gameRenderer.displayItemActivation(itemStack);


            player.swing(InteractionHand.MAIN_HAND, true);
        }

    }
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§dDepreciated"));
        tooltip.add(Component.literal("§7This item no longer works"));
    }
}
