package net.bandit.darkdoppelganger.item;

import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.bandit.darkdoppelganger.DarkDoppelgangerMod;
import net.bandit.darkdoppelganger.entity.DarkDoppelgangerEntity;
import net.bandit.darkdoppelganger.entity.EntityRegistry;
import net.bandit.darkdoppelganger.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class ShadowOrbItem extends Item {
    public ShadowOrbItem(Properties properties) {
        super(properties);
    }

    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        tagWithThrower(stack, player);
        super.onCraftedBy(stack, level, player);
        DarkDoppelgangerMod.LOGGER.info("Tagged Shadow Orb with UUID: {}", player.getUUID());
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        Level level = entity.level();

        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            if (!stack.hasTag() || !stack.getTag().hasUUID("ThrowerUUID")) {
                Player nearest = level.getNearestPlayer(entity, 5);
                if (nearest != null) {
                    tagWithThrower(stack, nearest);
                    DarkDoppelgangerMod.LOGGER.info("Fallback tagged Shadow Orb with UUID: {}", nearest.getUUID());
                }
            }

            if (level.dimension() == Level.END && entity.getY() < 0) {
                UUID throwerId = stack.getTag() != null && stack.getTag().hasUUID("ThrowerUUID")
                        ? stack.getTag().getUUID("ThrowerUUID")
                        : null;

                if (throwerId != null) {
                    Player player = serverLevel.getPlayerByUUID(throwerId);
                    if (player != null) {
                        level.playSound(null, player.blockPosition(), ModSounds.BOSS_LAUGH.get(), SoundSource.HOSTILE, 2.0F, 0.8F);

                        MinecraftServer server = serverLevel.getServer();
                        server.execute(() -> server.execute(() -> {
                            try {
                                Thread.sleep(4000);
                            } catch (InterruptedException ignored) {
                            }
                            summonDoppelganger(serverLevel, player);
                        }));
                    } else {
                        DarkDoppelgangerMod.LOGGER.warn("Shadow Orb triggered in void, but thrower not found.");
                    }
                } else {
                    DarkDoppelgangerMod.LOGGER.warn("Shadow Orb triggered in void, but thrower UUID was null.");
                }

                entity.discard();
            }
        }
        return false;
    }

    public static void tagWithThrower(ItemStack stack, Player player) {
        stack.getOrCreateTag().putUUID("ThrowerUUID", player.getUUID());
    }

    private void summonDoppelganger(ServerLevel level, Player player) {
        Vec3 forward = player.getLookAngle().normalize().scale(3);
        Vec3 spawnPos = player.position().add(forward).add(0, 1, 0);

        BlockPos groundCheck = new BlockPos((int) spawnPos.x, (int) (spawnPos.y - 1), (int) spawnPos.z);
        BlockState stateBelow = level.getBlockState(groundCheck);

        if (stateBelow.isAir() || !stateBelow.isSolidRender(level, groundCheck)) {
            spawnPos = player.position();
        }

        DarkDoppelgangerEntity boss = new DarkDoppelgangerEntity(EntityRegistry.DARK_DOPPELGANGER.get(), level);
        boss.setPos(spawnPos);
        boss.setCustomName(Component.literal(player.getName().getString()));
        boss.setCustomNameVisible(true);
        boss.setSummonerPlayer(player);
        boss.addTag("dark_doppelganger_boss");

        boss.setItemInHand(InteractionHand.MAIN_HAND, player.getMainHandItem().copy());
        boss.setItemInHand(InteractionHand.OFF_HAND, player.getOffhandItem().copy());

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.ARMOR) {
                ItemStack playerItem = player.getItemBySlot(slot);
                ItemStack itemToEquip;

                if (!playerItem.isEmpty()) {
                    itemToEquip = playerItem.copy();
                } else {
                    itemToEquip = switch (slot) {
                        case HEAD -> new ItemStack(ItemRegistry.NETHERITE_MAGE_HELMET.get());
                        case CHEST -> new ItemStack(ItemRegistry.NETHERITE_MAGE_CHESTPLATE.get());
                        case LEGS -> new ItemStack(ItemRegistry.NETHERITE_MAGE_LEGGINGS.get());
                        case FEET -> new ItemStack(ItemRegistry.NETHERITE_MAGE_BOOTS.get());
                        default -> ItemStack.EMPTY;
                    };
                }

                boss.setItemSlot(slot, itemToEquip);
            }
        }

// Example for weapons:
        if (player.getMainHandItem().isEmpty()) {
            boss.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemRegistry.ARTIFICER_STAFF.get()));
        } else {
            boss.setItemInHand(InteractionHand.MAIN_HAND, player.getMainHandItem().copy());
        }

        if (player.getOffhandItem().isEmpty()) {
            boss.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD));
        } else {
            boss.setItemInHand(InteractionHand.OFF_HAND, player.getOffhandItem().copy());
        }

// Now finally spawn the boss
        level.addFreshEntity(boss);


        // Particle swirl
        level.sendParticles(ParticleTypes.SMOKE, boss.getX(), boss.getY(), boss.getZ(), 30, 0.5, 1.0, 0.5, 0.05);
        level.playSound(null, boss.blockPosition(), SoundEvents.ENDERMAN_STARE, SoundSource.HOSTILE, 1.0F, 0.5F);

        DarkDoppelgangerMod.LOGGER.info("Dark Doppelganger summoned for player: {}", player.getName().getString());
    }
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§5Throw into the void...").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.literal("§7Only in The End. Something awaits.").withStyle(ChatFormatting.GRAY));

    }
}
