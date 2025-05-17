package net.bandit.darkdoppelganger.event;

import net.bandit.darkdoppelganger.entity.DarkDoppelgangerEntity;
import net.bandit.darkdoppelganger.registry.EntityRegistry;
import net.bandit.darkdoppelganger.registry.SoundRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class SummonDoppelganger {
    public static int summonDoppelganger(CommandSourceStack source) {
        try {
            ServerLevel serverWorld = source.getLevel();

            Player nearestPlayer = serverWorld.getNearestPlayer(
                    TargetingConditions.forNonCombat().range(50),
                    source.getPosition().x(),
                    source.getPosition().y(),
                    source.getPosition().z()
            );

            if (nearestPlayer == null) {
                source.sendFailure(Component.literal("No players nearby to copy."));
                return 0;
            }

            source.sendSuccess(() -> Component.literal("Dark Doppelganger will spawn in 5 seconds, copying " + nearestPlayer.getName().getString() + "!"), true);

            serverWorld.getServer().submitAsync(() -> {
                try {
                    Thread.sleep(5000);
                    serverWorld.getServer().execute(() -> {
                        if (nearestPlayer.isAlive() && nearestPlayer.level() == serverWorld) {
                            summonDoppelganger(serverWorld, nearestPlayer);
                        } else {
                            source.sendFailure(Component.literal("Target player is no longer valid."));
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });

            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("An error occurred while executing the command."));
            return 0;
        }
    }

    private static void summonDoppelganger(ServerLevel serverWorld, Player player) {
        try {
            if (player == null || !player.isAlive()) {
                return;
            }

            Vec3 lookVector = player.getLookAngle();
            Vec3 initialSpawnPosition = player.position().add(lookVector.scale(4));

            BlockPos groundPos = new BlockPos((int) initialSpawnPosition.x, (int) initialSpawnPosition.y, (int) initialSpawnPosition.z);
            while (serverWorld.isEmptyBlock(groundPos.below()) && groundPos.getY() > serverWorld.getMinBuildHeight()) {
                groundPos = groundPos.below();
            }
            BlockPos spawnPosition = groundPos.above();

            DarkDoppelgangerEntity entity = new DarkDoppelgangerEntity(EntityRegistry.DARK_DOPPELGANGER.get(), serverWorld);
            entity.setPos(spawnPosition.getX() + 0.5, spawnPosition.getY(), spawnPosition.getZ() + 0.5);
            entity.setYRot(-player.getYRot());
            entity.setSummonerPlayer(player);
            entity.addTag("dark_doppelganger_boss");
            entity.setCustomName(Component.literal(player.getName().getString()));
            entity.setCustomNameVisible(true);

            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            serverWorld.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundRegistry.BOSS_LAUGH.get(), SoundSource.PLAYERS, 1.5F, 1.0F);

            serverWorld.addFreshEntity(entity);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
