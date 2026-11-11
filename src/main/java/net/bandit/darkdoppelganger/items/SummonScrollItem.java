package net.bandit.darkdoppelganger.items;

import net.bandit.darkdoppelganger.registry.SoundRegistry;
import net.bandit.darkdoppelganger.summoning.DelayedTasks;
import net.bandit.darkdoppelganger.summoning.DoppelgangerSummoning;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SummonScrollItem extends Item {
    private static final int SUMMON_DELAY_TICKS = 100;

    public SummonScrollItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        Player player = context.getPlayer();
        ItemStack itemStack = context.getItemInHand();

        if (!world.isClientSide && player instanceof ServerPlayer serverPlayer) {
            player.sendSystemMessage(Component.literal("Dark Doppelganger will spawn in 5 seconds!"));

            ServerLevel serverWorld = (ServerLevel) world;
            BlockPos ritualPos = context.getClickedPos();
            boolean refund = !player.isCreative();
            DelayedTasks.schedule(serverWorld.getServer(), SUMMON_DELAY_TICKS,
                    () -> summonDoppelganger(serverWorld, serverPlayer, ritualPos, refund));

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
            player.playSound(SoundEvents.PORTAL_TRIGGER, 0.5F, 1.5F);
            Minecraft.getInstance().gameRenderer.displayItemActivation(itemStack);
            player.swing(InteractionHand.MAIN_HAND, true);
        }
    }

    private void summonDoppelganger(ServerLevel serverWorld, ServerPlayer player, BlockPos ritualPos, boolean refund) {
        if (!player.isAlive() || player.isRemoved() || player.level() != serverWorld) {
            return;
        }

        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
        serverWorld.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundRegistry.BOSS_LAUGH.get(), SoundSource.PLAYERS, 1.5F, 1.0F);

        if (!DoppelgangerSummoning.summon(serverWorld, player, ritualPos)) {
            player.displayClientMessage(Component.literal("No room for the Dark Doppelganger here."), true);
            if (refund) {
                player.getInventory().placeItemBackInInventory(new ItemStack(this));
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.literal("§dRight-click to summon the Dark Doppelganger after a 5-second countdown!"));
        tooltipComponents.add(Component.literal("§7Prepare warrior."));
    }
}
