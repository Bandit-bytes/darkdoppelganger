package net.bandit.darkdoppelganger.items;

import net.bandit.darkdoppelganger.entity.DarkDoppelgangerEntity;
import net.bandit.darkdoppelganger.registry.EntityRegistry;
import net.bandit.darkdoppelganger.registry.SoundRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SummonScrollItem extends Item {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public SummonScrollItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        Player player = context.getPlayer();
        ItemStack itemStack = context.getItemInHand();

        if (!world.isClientSide && player != null) {
            player.sendSystemMessage(Component.literal("Dark Doppelganger will spawn in 5 seconds!"));

            ServerLevel serverWorld = (ServerLevel) world;

            // Schedule the summoning task after 5 seconds
            scheduler.schedule(() -> serverWorld.getServer().execute(() -> summonDoppelganger(serverWorld, player)), 5, TimeUnit.SECONDS);

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

    private void summonDoppelganger(ServerLevel serverWorld, Player player) {
        Vec3 lookVector = player.getLookAngle();
        Vec3 spawnPosition = player.position().add(lookVector.scale(4));

        DarkDoppelgangerEntity entity = new DarkDoppelgangerEntity(EntityRegistry.DARK_DOPPELGANGER.get(), serverWorld);
        entity.setPos(spawnPosition.x, player.getY(), spawnPosition.z);
        entity.setYRot(-player.getYRot());
        entity.setSummonerPlayer(player);
        entity.addTag("dark_doppelganger_boss");
        entity.setCustomName(Component.literal(player.getName().getString()));
        entity.setCustomNameVisible(true);

        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
        serverWorld.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundRegistry.BOSS_LAUGH.get(), SoundSource.PLAYERS, 1.5F, 1.0F);

        serverWorld.addFreshEntity(entity);
    }
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.literal("§dRight-click to summon the Dark Doppelganger after a 5-second countdown!"));
        tooltipComponents.add(Component.literal("§7Prepare warrior."));
    }
}
