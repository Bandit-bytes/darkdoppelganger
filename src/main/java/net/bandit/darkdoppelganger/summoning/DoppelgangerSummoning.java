package net.bandit.darkdoppelganger.summoning;

import net.bandit.darkdoppelganger.DarkDoppelgangerMod;
import net.bandit.darkdoppelganger.entity.DarkDoppelgangerEntity;
import net.bandit.darkdoppelganger.entity.DarkDoppelgangerEquipmentHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class DoppelgangerSummoning {

    private DoppelgangerSummoning() {
    }

    public static boolean summon(ServerLevel level, Player player, BlockPos ritualPos) {
        if (player == null || !player.isAlive()) {
            return false;
        }

        DarkDoppelgangerEntity boss = new DarkDoppelgangerEntity(
                DarkDoppelgangerMod.DARK_DOPPELGANGER.get(),
                level
        );

        Vec3 spawnPosition = findSpawnPosition(level, boss, player, ritualPos);
        if (spawnPosition == null) {
            return false;
        }

        boss.setPos(spawnPosition.x, spawnPosition.y, spawnPosition.z);
        boss.setYRot(facePlayerYaw(spawnPosition, player.position()));
        boss.setYHeadRot(boss.getYRot());
        boss.setCustomName(Component.literal(player.getName().getString()));
        boss.setCustomNameVisible(true);
        boss.setSummonerPlayer(player);
        boss.addTag("dark_doppelganger_boss");
        DarkDoppelgangerEquipmentHelper.applyPlayerLoadout(boss, player);
        boss.setPersistenceRequired();

        if (!level.addFreshEntity(boss)) {
            return false;
        }

        level.sendParticles(
                net.minecraft.core.particles.ParticleTypes.SMOKE,
                boss.getX(), boss.getY() + 0.8, boss.getZ(),
                40, 0.55, 1.0, 0.55, 0.05
        );
        level.sendParticles(
                net.minecraft.core.particles.ParticleTypes.PORTAL,
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
        return true;
    }

    private static Vec3 findSpawnPosition(
            ServerLevel level,
            DarkDoppelgangerEntity boss,
            Player player,
            BlockPos ritualPos
    ) {
        Vec3 altarCenter = Vec3.atCenterOf(ritualPos);
        Vec3 away = altarCenter.subtract(player.position());
        away = new Vec3(away.x, 0.0, away.z);
        if (away.lengthSqr() < 0.001) {
            Vec3 look = player.getLookAngle();
            away = new Vec3(-look.x, 0.0, -look.z);
        }
        if (away.lengthSqr() < 0.001) {
            away = new Vec3(0.0, 0.0, 1.0);
        }
        away = away.normalize();

        Vec3 perpendicular = new Vec3(-away.z, 0.0, away.x);
        List<Vec3> offsets = new ArrayList<>();
        offsets.add(away.scale(2.5));
        offsets.add(perpendicular.scale(2.5));
        offsets.add(perpendicular.scale(-2.5));
        offsets.add(away.scale(-2.5));
        offsets.add(away.scale(3.5));
        offsets.add(perpendicular.scale(3.5));
        offsets.add(perpendicular.scale(-3.5));

        for (Vec3 offset : offsets) {
            double x = ritualPos.getX() + 0.5 + offset.x;
            double z = ritualPos.getZ() + 0.5 + offset.z;
            Vec3 candidate = findGroundedPosition(level, x, z, ritualPos.getY());
            if (candidate == null) {
                continue;
            }

            boss.setPos(candidate.x, candidate.y, candidate.z);
            if (level.getWorldBorder().isWithinBounds(boss.blockPosition()) && level.noCollision(boss)) {
                return candidate;
            }
        }

        return null;
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
