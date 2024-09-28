package net.bandit.darkdoppelganger.item;

import net.bandit.darkdoppelganger.entity.DarkDoppelgangerEntity;
import net.bandit.darkdoppelganger.entity.EntityRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
            // Server-side: Announce the countdown
            player.sendSystemMessage(Component.literal("Dark Doppelganger will spawn in 5 seconds!"));

            // Start a countdown using delayed server tasks
            ServerLevel serverWorld = (ServerLevel) world;
            serverWorld.getServer().submitAsync(() -> {
                // Delay for 5 seconds
                try {
                    Thread.sleep(5000); // Delay for 5 seconds
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // After 5 seconds, summon the boss
                serverWorld.getServer().execute(() -> {
                    summonDoppelganger(serverWorld, player);
                });
            });

            // Shrink the item stack after usage (for non-creative players)
            if (!player.isCreative()) {
                itemStack.shrink(1); // Reduce the item stack by 1
            }

            return InteractionResult.SUCCESS;
        }

        // Client-side: Trigger Totem of Undying animation (run only on client side)
        if (world.isClientSide) {
            triggerTotemAnimation(player, itemStack);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    // Helper method to trigger the Totem of Undying animation
    private void triggerTotemAnimation(Player player, ItemStack itemStack) {
        if (player.level().isClientSide()) {
            // Play the totem use sound
            player.playSound(SoundEvents.TOTEM_USE, 1.0F, 1.0F);

            // Trigger the totem animation (spins in front of the player's face)
            Minecraft.getInstance().gameRenderer.displayItemActivation(itemStack);

            // Swing the player's hand (to simulate using the item)
            player.swing(InteractionHand.MAIN_HAND, true);
        }
    }

    // Helper method to summon the Dark Doppelganger entity
    private void summonDoppelganger(ServerLevel serverWorld, Player player) {
        DarkDoppelgangerEntity entity = new DarkDoppelgangerEntity(EntityRegistry.DARK_DOPPELGANGER.get(), serverWorld);
        entity.setPos(player.getX() + 2, player.getY(), player.getZ() + 2);  // Spawn next to the player

        // Set summoner information and display name
        entity.setSummonerPlayer(player);
        entity.setCustomName(Component.literal(player.getName().getString()));
        entity.setCustomNameVisible(true);

        // Play summon sound
        serverWorld.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 1.0F, 0.2F);

        // Add the entity to the world
        serverWorld.addFreshEntity(entity);
    }
    // Add a tooltip explaining the behavior of the scroll
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§dRight-click to summon the Dark Doppelganger after a 5-second countdown!"));
        tooltip.add(Component.literal("§7Prepare warrior."));
    }
}
