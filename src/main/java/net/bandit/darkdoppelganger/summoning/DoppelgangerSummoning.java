package net.bandit.darkdoppelganger.summoning;

import net.bandit.darkdoppelganger.Config;
import net.bandit.darkdoppelganger.cutscene.CutsceneTracker;
import net.bandit.darkdoppelganger.entity.DarkDoppelgangerEntity;
import net.bandit.darkdoppelganger.registry.EntityRegistry;
import net.bandit.darkdoppelganger.registry.SoundRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class DoppelgangerSummoning {
    /** Distances past the ritual spot to try first, and the turns off straight ahead to try at each. */
    private static final double[] FAR_DISTANCES = {10.0, 8.0, 6.0};
    private static final double[] FAR_ANGLES = {0.0, 20.0, -20.0, 40.0, -40.0};

    private DoppelgangerSummoning() {
    }

    /** Summons the boss for this player. Returns null when it could not be placed. */
    @Nullable
    public static DarkDoppelgangerEntity summon(ServerLevel level, Player player, BlockPos ritualPos) {
        if (player == null || !player.isAlive()) {
            return null;
        }

        DarkDoppelgangerEntity boss = EntityRegistry.DARK_DOPPELGANGER.get().create(level);
        if (boss == null) {
            return null;
        }

        Vec3 spawnPosition = findSpawnPosition(level, boss, player, ritualPos);
        if (spawnPosition == null) {
            return null;
        }

        boss.setPos(spawnPosition.x, spawnPosition.y, spawnPosition.z);
        boss.setYRot(facePlayerYaw(spawnPosition, player.position()));
        boss.setYHeadRot(boss.getYRot());
        boss.setCustomName(Component.literal(player.getName().getString()));
        boss.setCustomNameVisible(true);
        boss.setSummonerPlayer(player);
        boss.addTag("dark_doppelganger_boss");

        applyConfiguredEquipment(boss, player);
        disableEquipmentDrops(boss);
        boss.setPersistenceRequired();
        boss.prepareEntrance(Config.INTRO_CUTSCENE.get() ? CutsceneTracker.nearbyViewers(boss) : List.of());

        if (!level.addFreshEntity(boss)) {
            return null;
        }
        // During the intro the boss makes its own entrance when it emerges.
        if (boss.isInIntro()) {
            return boss;
        }

        level.sendParticles(
                ParticleTypes.SMOKE,
                boss.getX(), boss.getY() + 0.8, boss.getZ(),
                40, 0.55, 1.0, 0.55, 0.05
        );
        level.sendParticles(
                ParticleTypes.PORTAL,
                boss.getX(), boss.getY() + 0.8, boss.getZ(),
                50, 0.6, 1.0, 0.6, 0.15
        );
        level.playSound(
                null,
                boss.blockPosition(),
                SoundEvents.ENDERMAN_STARE,
                SoundSource.HOSTILE,
                1.0F,
                0.5F
        );
        return boss;
    }

    /**
     * Slows the summoner and plays the intro line at them. The Summon Scroll and the summon command use
     * it when there is no intro. The intro plays its own line and holds the player still.
     */
    public static void greetSummoner(ServerLevel level, Player player) {
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundRegistry.VOICE_INTRO.get(), SoundSource.PLAYERS, 1.5F, 1.0F);
    }

    private static void applyConfiguredEquipment(DarkDoppelgangerEntity boss, Player player) {
        ItemStack mainHand;
        if (Config.DOPPELGANGER_COPY_PLAYER_MAINHAND.get()) {
            ItemStack playerMainHand = player.getMainHandItem();
            mainHand = playerMainHand.isEmpty()
                    ? getConfiguredItem(Config.DOPPELGANGER_DEFAULT_MAINHAND.get())
                    : playerMainHand.copy();
        } else {
            mainHand = getConfiguredItem(Config.DOPPELGANGER_DEFAULT_MAINHAND.get());
        }
        boss.setItemSlot(EquipmentSlot.MAINHAND, mainHand);

        List<? extends String> banned = Config.DOPPELGANGER_BANNED_ARMOR.get();
        boolean copyPlayerArmor = Config.DOPPELGANGER_COPY_PLAYER_ARMOR.get();

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) {
                continue;
            }

            ItemStack equipment;
            if (!copyPlayerArmor) {
                equipment = getConfiguredArmorForSlot(slot);
            } else {
                ItemStack playerArmor = player.getItemBySlot(slot);
                equipment = !playerArmor.isEmpty() && !isArmorBanned(playerArmor, banned)
                        ? playerArmor.copy()
                        : getConfiguredArmorForSlot(slot);
            }
            boss.setItemSlot(slot, equipment);
        }
    }

    /** The boss's own gear for a slot, from the config: what it wears before it copies the summoner. */
    public static ItemStack ownItem(EquipmentSlot slot) {
        return switch (slot) {
            case MAINHAND -> getConfiguredItem(Config.DOPPELGANGER_DEFAULT_MAINHAND.get());
            case HEAD, CHEST, LEGS, FEET -> getConfiguredArmorForSlot(slot);
            default -> ItemStack.EMPTY;
        };
    }

    private static ItemStack getConfiguredArmorForSlot(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> getConfiguredItem(Config.DOPPELGANGER_DEFAULT_HELMET.get());
            case CHEST -> getConfiguredItem(Config.DOPPELGANGER_DEFAULT_CHESTPLATE.get());
            case LEGS -> getConfiguredItem(Config.DOPPELGANGER_DEFAULT_LEGGINGS.get());
            case FEET -> getConfiguredItem(Config.DOPPELGANGER_DEFAULT_BOOTS.get());
            default -> ItemStack.EMPTY;
        };
    }

    private static ItemStack getConfiguredItem(String id) {
        ResourceLocation resourceLocation = ResourceLocation.tryParse(id);
        if (resourceLocation == null) {
            return ItemStack.EMPTY;
        }

        Item item = BuiltInRegistries.ITEM.get(resourceLocation);
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    private static boolean isArmorBanned(ItemStack stack, List<? extends String> bannedList) {
        if (stack.isEmpty()) {
            return false;
        }

        String itemId = stack.getItem().builtInRegistryHolder().key().location().toString();
        for (String banned : bannedList) {
            if (banned.endsWith("*")) {
                if (itemId.startsWith(banned.substring(0, banned.length() - 1))) {
                    return true;
                }
            } else if (itemId.equals(banned)) {
                return true;
            }
        }
        return false;
    }

    private static void disableEquipmentDrops(DarkDoppelgangerEntity boss) {
        boss.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        boss.setDropChance(EquipmentSlot.OFFHAND, 0.0F);
        boss.setDropChance(EquipmentSlot.HEAD, 0.0F);
        boss.setDropChance(EquipmentSlot.CHEST, 0.0F);
        boss.setDropChance(EquipmentSlot.LEGS, 0.0F);
        boss.setDropChance(EquipmentSlot.FEET, 0.0F);
    }

    /**
     * Picks where the boss enters: in front of the summoner, well past the ritual spot, where the
     * summoner can see it. Far enough that everyone gathered at the altar sees it in front of them.
     * Falls back to spots near the ritual, then to far spots out of sight.
     */
    private static Vec3 findSpawnPosition(
            ServerLevel level,
            DarkDoppelgangerEntity boss,
            Player player,
            BlockPos ritualPos
    ) {
        Vec3 altarCenter = Vec3.atCenterOf(ritualPos);
        Vec3 forward = altarCenter.subtract(player.position());
        forward = new Vec3(forward.x, 0.0, forward.z);

        // The summoner is on the ritual spot (the command run at their feet): use where they look.
        if (forward.lengthSqr() < 1.0) {
            Vec3 look = player.getLookAngle();
            forward = new Vec3(look.x, 0.0, look.z);
        }
        if (forward.lengthSqr() < 0.001) {
            forward = new Vec3(0.0, 0.0, 1.0);
        }
        forward = forward.normalize();

        Vec3 hidden = null;
        for (double distance : FAR_DISTANCES) {
            for (double angle : FAR_ANGLES) {
                Vec3 offset = forward.yRot((float) Math.toRadians(angle)).scale(distance);
                Vec3 candidate = findFreeSpot(level, boss, ritualPos, offset);
                if (candidate == null) continue;
                if (CutsceneTracker.canSee(player, candidate)) return candidate;
                if (hidden == null) hidden = candidate;
            }
        }

        Vec3 perpendicular = new Vec3(-forward.z, 0.0, forward.x);
        List<Vec3> nearOffsets = new ArrayList<>();
        nearOffsets.add(forward.scale(2.5));
        nearOffsets.add(perpendicular.scale(2.5));
        nearOffsets.add(perpendicular.scale(-2.5));
        nearOffsets.add(forward.scale(-2.5));
        nearOffsets.add(forward.scale(3.5));
        nearOffsets.add(perpendicular.scale(3.5));
        nearOffsets.add(perpendicular.scale(-3.5));
        for (Vec3 offset : nearOffsets) {
            Vec3 candidate = findFreeSpot(level, boss, ritualPos, offset);
            if (candidate != null) return candidate;
        }
        return hidden;
    }

    @Nullable
    private static Vec3 findFreeSpot(ServerLevel level, DarkDoppelgangerEntity boss, BlockPos ritualPos, Vec3 offset) {
        double x = ritualPos.getX() + 0.5 + offset.x;
        double z = ritualPos.getZ() + 0.5 + offset.z;
        Vec3 candidate = findGroundedPosition(level, x, z, ritualPos.getY());
        if (candidate == null) return null;
        boss.setPos(candidate.x, candidate.y, candidate.z);
        return level.getWorldBorder().isWithinBounds(boss.blockPosition()) && level.noCollision(boss) ? candidate : null;
    }

    private static Vec3 findGroundedPosition(ServerLevel level, double x, double z, int originY) {
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);

        for (int floorY = originY + 3; floorY >= originY - 4; floorY--) {
            BlockPos floorPos = new BlockPos(blockX, floorY, blockZ);
            BlockState floorState = level.getBlockState(floorPos);
            if (!floorState.isFaceSturdy(level, floorPos, Direction.UP)) {
                continue;
            }

            BlockPos feetPos = floorPos.above();
            if (!level.getBlockState(feetPos).getCollisionShape(level, feetPos).isEmpty()) {
                continue;
            }
            BlockPos headPos = feetPos.above();
            if (!level.getBlockState(headPos).getCollisionShape(level, headPos).isEmpty()) {
                continue;
            }

            return new Vec3(blockX + 0.5, feetPos.getY(), blockZ + 0.5);
        }

        return null;
    }

    private static float facePlayerYaw(Vec3 from, Vec3 target) {
        double dx = target.x - from.x;
        double dz = target.z - from.z;
        return (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
    }
}
