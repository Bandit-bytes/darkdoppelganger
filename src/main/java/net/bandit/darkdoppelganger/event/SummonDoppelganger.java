package net.bandit.darkdoppelganger.event;

import net.bandit.darkdoppelganger.registry.SoundRegistry;
import net.bandit.darkdoppelganger.summoning.DoppelgangerSummoning;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;

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

            BlockPos ritualPos = BlockPos.containing(
                    source.getPosition().x(),
                    source.getPosition().y(),
                    source.getPosition().z()
            );

            source.sendSuccess(
                    () -> Component.literal(
                            "Dark Doppelganger will spawn in 5 seconds, copying "
                                    + nearestPlayer.getName().getString()
                                    + "!"
                    ),
                    true
            );

            serverWorld.getServer().submitAsync(() -> {
                try {
                    Thread.sleep(5000);
                    serverWorld.getServer().execute(() -> {
                        if (nearestPlayer.isAlive() && nearestPlayer.level() == serverWorld) {
                            nearestPlayer.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                            serverWorld.playSound(
                                    null,
                                    nearestPlayer.getX(),
                                    nearestPlayer.getY(),
                                    nearestPlayer.getZ(),
                                    SoundRegistry.BOSS_LAUGH.get(),
                                    SoundSource.PLAYERS,
                                    1.5F,
                                    1.0F
                            );

                            if (!DoppelgangerSummoning.summon(serverWorld, nearestPlayer, ritualPos)) {
                                source.sendFailure(Component.literal("No safe place was found for the Dark Doppelganger."));
                            }
                        } else {
                            source.sendFailure(Component.literal("Target player is no longer valid."));
                        }
                    });
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
            });

            return 1;
        } catch (Exception exception) {
            source.sendFailure(Component.literal("An error occurred while executing the command."));
            return 0;
        }
    }
}
