package net.bandit.darkdoppelganger.item;

import net.bandit.darkdoppelganger.entity.DarkDoppelgangerEntity;
import net.bandit.darkdoppelganger.entity.EntityRegistry;
import net.bandit.darkdoppelganger.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
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
            player.sendSystemMessage(Component.literal("Dark Doppelganger will spawn in 5 seconds!"));

            ServerLevel serverWorld = (ServerLevel) world;
            serverWorld.getServer().submitAsync(() -> {
                // Delay for 5 seconds
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                serverWorld.getServer().execute(() -> {
                    summonDoppelganger(serverWorld, player);
                });
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

    private void summonDoppelganger(ServerLevel serverWorld, Player player) {
        double summonX = player.getX();
        double summonY = player.getY() + 10;
        double summonZ = player.getZ();

        // Create the entity
        DarkDoppelgangerEntity entity = new DarkDoppelgangerEntity(EntityRegistry.DARK_DOPPELGANGER.get(), serverWorld);
        entity.setPos(summonX, summonY, summonZ);

        entity.setSummonerPlayer(player);
        entity.setCustomName(Component.literal(player.getName().getString()));
        entity.setCustomNameVisible(true);

        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));

        serverWorld.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.BOSS_LAUGH.get(), SoundSource.PLAYERS, 1.5F, 1.0F);

        serverWorld.addFreshEntity(entity);
        serverWorld.getServer().execute(() -> {
            double teleportX = player.getX() + (serverWorld.random.nextDouble() - 0.5) * 5.0;
            double teleportY = player.getY();
            double teleportZ = player.getZ() + (serverWorld.random.nextDouble() - 0.5) * 5.0;

            entity.teleportTo(teleportX, teleportY, teleportZ);
            serverWorld.playSound(null, teleportX, teleportY, teleportZ,
                    ModSounds.BOSS_LAUGH.get(), SoundSource.HOSTILE, 1.5F, 0.8F);

        });
    }
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§dRight-click to summon the Dark Doppelganger after a 5-second countdown!"));
        tooltip.add(Component.literal("§7Prepare warrior."));
    }
}
