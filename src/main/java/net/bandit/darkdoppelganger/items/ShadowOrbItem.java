package net.bandit.darkdoppelganger.items;

import com.mojang.serialization.Codec;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.bandit.darkdoppelganger.entity.DarkDoppelgangerEntity;
import net.bandit.darkdoppelganger.registry.EntityRegistry;
import net.bandit.darkdoppelganger.registry.SoundRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
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


import java.util.List;
import java.util.UUID;

public class ShadowOrbItem extends Item {
    public static final DataComponentType<UUID> THROWER_UUID = DataComponentType.<UUID>builder()
            .persistent(Codec.STRING.xmap(UUID::fromString, UUID::toString))
//            .networkSynchronized(StreamCodec.uuid())
            .build();

    public ShadowOrbItem(Properties properties) {
        super(properties);
    }

    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        stack.set(THROWER_UUID, player.getUUID());
        super.onCraftedBy(stack, level, player);
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        Level level = entity.level();

        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            UUID throwerId = stack.get(THROWER_UUID);
            if (throwerId == null) {
                Player nearest = level.getNearestPlayer(entity, 5);
                if (nearest != null) {
                    stack.set(THROWER_UUID, nearest.getUUID());
                    throwerId = nearest.getUUID();
                }
            }

            if (level.dimension() == Level.END && entity.getY() < 0) {
                if (throwerId != null) {
                    Player player = serverLevel.getPlayerByUUID(throwerId);
                    if (player != null) {
                        level.playSound(null, player.blockPosition(), SoundRegistry.BOSS_LAUGH.get(), SoundSource.HOSTILE, 2.0F, 0.8F);

                        serverLevel.getServer().execute(() -> {
                            try {
                                Thread.sleep(4000);
                            } catch (InterruptedException ignored) {
                            }
                            summonDoppelganger(serverLevel, player);
                        });
                    }
                }
                entity.discard();
            }
        }
        return false;
    }
    private void summonDoppelganger(ServerLevel level, Player player) {
        Vec3 forward = player.getLookAngle().normalize().scale(3);
        Vec3 spawnPos = player.position().add(forward).add(0, 1, 0);

        BlockPos groundCheck = BlockPos.containing(spawnPos.x, spawnPos.y - 1, spawnPos.z);
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

        // Copy or assign default main hand weapon
        ItemStack mainHand = player.getMainHandItem();
        boss.setItemInHand(InteractionHand.MAIN_HAND,
                mainHand.isEmpty() ? new ItemStack(ItemRegistry.ARTIFICER_STAFF) : mainHand.copy());
        // boss.setItemInHand(InteractionHand.OFF_HAND, ...)

//        for (EquipmentSlot slot : EquipmentSlot.values()) {
//            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
//                ItemStack playerItem = player.getItemBySlot(slot);
//                ItemStack armorToEquip;
//
//                if (!playerItem.isEmpty()) {
//                    armorToEquip = playerItem.copyWithCount(playerItem.getCount());
//                } else {
//                    armorToEquip = switch (slot) {
//                        case HEAD -> new ItemStack(ItemRegistry.NETHERITE_MAGE_HELMET);
//                        case CHEST -> new ItemStack(ItemRegistry.NETHERITE_MAGE_CHESTPLATE);
//                        case LEGS -> new ItemStack(ItemRegistry.NETHERITE_MAGE_LEGGINGS);
//                        case FEET -> new ItemStack(ItemRegistry.NETHERITE_MAGE_BOOTS);
//                        default -> ItemStack.EMPTY;
//                    };
//                }
//
//                boss.setItemSlot(slot, armorToEquip);
        boss.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemRegistry.ARTIFICER_STAFF));

// Always equip with fixed armor set
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                ItemStack armorToEquip = switch (slot) {
                    case HEAD -> new ItemStack(ItemRegistry.NETHERITE_MAGE_HELMET);
                    case CHEST -> new ItemStack(ItemRegistry.NETHERITE_MAGE_CHESTPLATE);
                    case LEGS -> new ItemStack(ItemRegistry.NETHERITE_MAGE_LEGGINGS);
                    case FEET -> new ItemStack(ItemRegistry.NETHERITE_MAGE_BOOTS);
                    default -> ItemStack.EMPTY;
                };

                boss.setItemSlot(slot, armorToEquip);
    }



        }
        level.addFreshEntity(boss);
        level.sendParticles(ParticleTypes.SMOKE, boss.getX(), boss.getY(), boss.getZ(), 30, 0.5, 1.0, 0.5, 0.05);
        level.playSound(null, boss.blockPosition(), SoundEvents.ENDERMAN_STARE, SoundSource.HOSTILE, 1.0F, 0.5F);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.literal("Throw into the void...").withStyle(ChatFormatting.DARK_PURPLE));
        tooltipComponents.add(Component.literal("Only in The End. Something awaits.").withStyle(ChatFormatting.GRAY));
    }
}