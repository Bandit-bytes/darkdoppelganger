package net.bandit.darkdoppelganger.items;

import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.bandit.darkdoppelganger.Config;
import net.bandit.darkdoppelganger.entity.DarkDoppelgangerEntity;
import net.bandit.darkdoppelganger.registry.EntityRegistry;
import net.bandit.darkdoppelganger.registry.SoundRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

import static net.bandit.darkdoppelganger.registry.ComponentRegistry.THROWER_UUID;

public class ShadowOrbItem extends Item {

    // Entity persistent data keys
    private static final String E_QUEUED   = "DoppelSummonQueued";
    private static final String E_DELAY    = "DoppelSummonDelay";
    private static final String E_ANCHOR_X = "DoppelAnchorX";
    private static final String E_ANCHOR_Y = "DoppelAnchorY";
    private static final String E_ANCHOR_Z = "DoppelAnchorZ";
    private static final String E_ANGLE    = "DoppelAngle";
    private static final String E_THROWER  = "DoppelThrowerUUID";

    private static final int SUMMON_DELAY_TICKS = 100;  // 5s
    private static final double VOID_TRIGGER_Y = 30.0;  // your 1.20.1 threshold
    private static final int NO_PICKUP_DELAY = 700;

    public ShadowOrbItem(Properties properties) {
        super(properties);
    }

    // -----
    // Throw on right click (1.20.1 feature)
    // -----
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);

        if (!level.isClientSide) {
            ItemStack thrown = held.copy();
            thrown.setCount(1);
            thrown.set(THROWER_UUID.get(), player.getUUID());

            ItemEntity orb = new ItemEntity(level,
                    player.getX(),
                    player.getEyeY() - 0.2,
                    player.getZ(),
                    thrown
            );

            orb.setPickUpDelay(NO_PICKUP_DELAY);

            Vec3 motion = player.getLookAngle().normalize().scale(0.85).add(0, 0.10, 0);
            orb.setDeltaMovement(motion);

            level.addFreshEntity(orb);

            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
        }

        return InteractionResultHolder.sidedSuccess(held, level.isClientSide);
    }

    // -----
    // Tag on craft (you already do this, keep it)
    // -----
    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        stack.set(THROWER_UUID.get(), player.getUUID());
        super.onCraftedBy(stack, level, player);
    }

    // -----
    // Update while it's an ItemEntity (queued hover + summon)
    // -----
    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        Level level = entity.level();
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) return false;

        entity.setPickUpDelay(NO_PICKUP_DELAY);

        // Ensure we have a thrower UUID if possible
        ensureThrowerOnce(stack, serverLevel, entity);

        CompoundTag e = entity.getPersistentData();

        // If queued, do hover/orbit + countdown, then summon
        if (e.getBoolean(E_QUEUED)) {
            entity.setExtendedLifetime();
            entity.setNoGravity(true);
            entity.setDeltaMovement(Vec3.ZERO);
            entity.setPickUpDelay(NO_PICKUP_DELAY);

            Vec3 anchor = new Vec3(e.getDouble(E_ANCHOR_X), e.getDouble(E_ANCHOR_Y), e.getDouble(E_ANCHOR_Z));

            float angle = e.getFloat(E_ANGLE) + 0.35f;
            e.putFloat(E_ANGLE, angle);

            double radius = 0.45;
            double ox = Math.cos(angle) * radius;
            double oz = Math.sin(angle) * radius;
            double bob = Math.sin(angle * 0.6) * 0.08;

            Vec3 pos = anchor.add(ox, bob, oz);
            entity.teleportTo(pos.x, pos.y, pos.z);

            serverLevel.sendParticles(ParticleTypes.SMOKE, pos.x, pos.y + 0.1, pos.z, 2, 0.05, 0.02, 0.05, 0.0);
            if ((entity.tickCount % 4) == 0) {
                serverLevel.sendParticles(ParticleTypes.PORTAL, pos.x, pos.y + 0.15, pos.z, 1, 0.02, 0.02, 0.02, 0.0);
            }
            if ((entity.tickCount % 20) == 0) {
                serverLevel.playSound(null, BlockPos.containing(pos), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.AMBIENT, 0.4F, 0.8F);
            }

            int delay = e.getInt(E_DELAY);
            if (delay > 0) {
                e.putInt(E_DELAY, delay - 1);
                return false;
            }

            Player thrower = getThrower(serverLevel, stack, entity);
            if (thrower != null && thrower.isAlive()) {
                serverLevel.sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
                serverLevel.playSound(null, BlockPos.containing(pos), SoundEvents.ENDERMAN_SCREAM, SoundSource.HOSTILE, 0.7F, 0.7F);
                summonDoppelganger(serverLevel, thrower);
            }

            entity.discard();
            return false;
        }

        // Trigger once when orb falls into the End void threshold
        if (serverLevel.dimension() == Level.END && entity.getY() < VOID_TRIGGER_Y) {
            UUID throwerId = stack.get(THROWER_UUID.get());
            if (throwerId != null) e.putUUID(E_THROWER, throwerId);

            Player thrower = (throwerId != null) ? serverLevel.getPlayerByUUID(throwerId) : null;

            if (thrower != null) {
                serverLevel.playSound(null, thrower.blockPosition(),
                        SoundRegistry.BOSS_LAUGH.get(), SoundSource.HOSTILE, 2.0F, 0.8F);
            }

            Vec3 anchor = (thrower != null)
                    ? getHoverAnchor(serverLevel, thrower)
                    : new Vec3(entity.getX(), VOID_TRIGGER_Y + 10.0, entity.getZ());

            e.putBoolean(E_QUEUED, true);
            e.putInt(E_DELAY, SUMMON_DELAY_TICKS);
            e.putDouble(E_ANCHOR_X, anchor.x);
            e.putDouble(E_ANCHOR_Y, anchor.y);
            e.putDouble(E_ANCHOR_Z, anchor.z);
            e.putFloat(E_ANGLE, serverLevel.random.nextFloat() * 6.2831855f);

            entity.setNoGravity(true);
            entity.setDeltaMovement(Vec3.ZERO);
            entity.teleportTo(anchor.x, anchor.y, anchor.z);
            entity.setExtendedLifetime();
            entity.setPickUpDelay(NO_PICKUP_DELAY);
        }

        return false;
    }

    private static void ensureThrowerOnce(ItemStack stack, ServerLevel level, ItemEntity entity) {
        UUID id = stack.get(THROWER_UUID.get());
        if (id != null) return;

        Player nearest = level.getNearestPlayer(entity, 2);
        if (nearest != null) {
            stack.set(THROWER_UUID.get(), nearest.getUUID());
        }
    }

    @Nullable
    private static Player getThrower(ServerLevel level, ItemStack stack, ItemEntity entity) {
        CompoundTag e = entity.getPersistentData();
        if (e.hasUUID(E_THROWER)) return level.getPlayerByUUID(e.getUUID(E_THROWER));

        UUID fromStack = stack.get(THROWER_UUID.get());
        return (fromStack != null) ? level.getPlayerByUUID(fromStack) : null;
    }

    private static Vec3 getHoverAnchor(ServerLevel level, Player player) {
        Vec3 forward = player.getLookAngle().normalize();
        Vec3 base = player.position().add(forward.scale(1.8)).add(0, 1.2, 0);

        BlockPos pos = BlockPos.containing(base);
        int groundY = findGroundY(level, pos, 12);
        if (groundY != Integer.MIN_VALUE) {
            base = new Vec3(base.x, groundY + 1.3, base.z);
        }
        return base;
    }

    private static int findGroundY(ServerLevel level, BlockPos start, int maxDown) {
        BlockPos.MutableBlockPos m = start.mutable();
        for (int i = 0; i <= maxDown; i++) {
            BlockPos below = m.below(i + 1);
            BlockState state = level.getBlockState(below);
            if (!state.isAir() && state.isSolidRender(level, below)) return below.getY();
        }
        return Integer.MIN_VALUE;
    }

    private static void summonDoppelganger(ServerLevel level, Player player) {
        List<? extends String> banned = Config.DOPPELGANGER_BANNED_ARMOR.get();

        // Spawn behind player like 1.20.1
        Vec3 back = player.getLookAngle().normalize().scale(-2.5);
        Vec3 spawnPos = player.position().add(back).add(0, 0.25, 0);

        BlockPos groundCheck = BlockPos.containing(spawnPos.x, spawnPos.y - 1, spawnPos.z);
        BlockState below = level.getBlockState(groundCheck);
        if (below.isAir() || !below.isSolidRender(level, groundCheck)) {
            spawnPos = player.position().add(0, 0.25, 0);
        }

        DarkDoppelgangerEntity boss = EntityRegistry.DARK_DOPPELGANGER.get().create(level);
        if (boss == null) return;

        boss.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        boss.setCustomName(Component.literal(player.getName().getString()));
        boss.setCustomNameVisible(true);
        boss.setSummonerPlayer(player);
        boss.addTag("dark_doppelganger_boss");

        // Copy main hand like 1.20.1
        boss.setItemSlot(EquipmentSlot.MAINHAND, player.getMainHandItem().copy());

        // Armor copy with banned list + fallback
        setArmorFromPlayer(boss, player, banned);

        // No gear drops
        boss.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        boss.setDropChance(EquipmentSlot.OFFHAND, 0.0F);
        boss.setDropChance(EquipmentSlot.HEAD, 0.0F);
        boss.setDropChance(EquipmentSlot.CHEST, 0.0F);
        boss.setDropChance(EquipmentSlot.LEGS, 0.0F);
        boss.setDropChance(EquipmentSlot.FEET, 0.0F);

        boss.setPersistenceRequired();

        boolean spawned = level.addFreshEntity(boss);
        if (spawned) {
            level.sendParticles(ParticleTypes.SMOKE, boss.getX(), boss.getY(), boss.getZ(), 30, 0.5, 1.0, 0.5, 0.05);
            level.playSound(null, boss.blockPosition(), SoundEvents.ENDERMAN_STARE, SoundSource.HOSTILE, 1.0F, 0.5F);
        }
    }

    private static void setArmorFromPlayer(DarkDoppelgangerEntity boss, Player player, List<? extends String> banned) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) continue;

            ItemStack playerArmor = player.getItemBySlot(slot);

            boolean bannedArmor = isArmorBanned(playerArmor, banned);

            ItemStack equip = (!playerArmor.isEmpty() && !bannedArmor)
                    ? playerArmor.copy()
                    : switch (slot) {
                case HEAD  -> new ItemStack(ItemRegistry.NETHERITE_MAGE_HELMET);
                case CHEST -> new ItemStack(ItemRegistry.NETHERITE_MAGE_CHESTPLATE);
                case LEGS  -> new ItemStack(ItemRegistry.NETHERITE_MAGE_LEGGINGS);
                case FEET  -> new ItemStack(ItemRegistry.NETHERITE_MAGE_BOOTS);
                default    -> ItemStack.EMPTY;
            };

            boss.setItemSlot(slot, equip);
        }
    }

    private static boolean isArmorBanned(ItemStack stack, List<? extends String> bannedList) {
        if (stack.isEmpty()) return false;

        String itemId = stack.getItem().builtInRegistryHolder().key().location().toString();
        for (String ban : bannedList) {
            if (ban.endsWith("*")) {
                if (itemId.startsWith(ban.substring(0, ban.length() - 1))) return true;
            } else if (itemId.equals(ban)) {
                return true;
            }
        }
        return false;
    }

    // 1.21.1 tooltip signature (your current file uses TooltipContext)
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.literal("Throw into the void...").withStyle(ChatFormatting.DARK_PURPLE));
        tooltipComponents.add(Component.literal("Only in The End. Something awaits.").withStyle(ChatFormatting.GRAY));
    }
}
