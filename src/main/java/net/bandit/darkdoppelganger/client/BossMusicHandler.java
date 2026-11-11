package net.bandit.darkdoppelganger.client;

import net.bandit.darkdoppelganger.DarkDoppelgangerMod;
import net.bandit.darkdoppelganger.entity.DarkDoppelgangerEntity;
import net.bandit.darkdoppelganger.registry.SoundRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.SelectMusicEvent;

/**
 * Plays the boss track through the music manager while a Dark Doppelganger is near the player.
 * The music manager plays it at full volume wherever the player stands, loops it, and keeps
 * vanilla tracks (including End music) from playing over it.
 */
@EventBusSubscriber(modid = DarkDoppelgangerMod.MOD_ID, value = Dist.CLIENT)
public final class BossMusicHandler {
    /** Matches the boss's tracking range, so music plays while the boss bar is shown. */
    private static final double RANGE = 80.0;
    private static final int SCAN_INTERVAL = 10;

    private static Music bossMusic;
    private static boolean fightNearby;

    private BossMusicHandler() {
    }

    private static Music bossMusic() {
        if (bossMusic == null) {
            bossMusic = new Music(SoundRegistry.BOSS_FIGHT_MUSIC, 0, 0, true);
        }
        return bossMusic;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            fightNearby = false;
            return;
        }
        if (mc.level.getGameTime() % SCAN_INTERVAL != 0) return;

        boolean wasNearby = fightNearby;
        fightNearby = !mc.level.getEntitiesOfClass(DarkDoppelgangerEntity.class,
                mc.player.getBoundingBox().inflate(RANGE), LivingEntity::isAlive).isEmpty();
        if (wasNearby && !fightNearby) {
            // Stop the sound directly so the music manager waits its normal delay before vanilla music returns.
            mc.getSoundManager().stop(SoundRegistry.BOSS_FIGHT_MUSIC.get().getLocation(), SoundSource.MUSIC);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onSelectMusic(SelectMusicEvent event) {
        if (fightNearby) {
            event.overrideMusic(bossMusic());
        }
    }
}
