package net.bandit.darkdoppelganger.client;

import net.bandit.darkdoppelganger.DarkDoppelgangerMod;
import net.bandit.darkdoppelganger.entity.DarkDoppelgangerEntity;
import net.bandit.darkdoppelganger.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

/**
 * Plays the boss track while a Dark Doppelganger is fighting near the player: full volume wherever
 * the player stands, looped, and lowered while the boss speaks so its lines can be heard.
 * Vanilla music is held off during the intro and the fight, and for a few minutes afterwards, as the
 * music manager would after any track.
 */
@Mod.EventBusSubscriber(modid = DarkDoppelgangerMod.MOD_ID, value = Dist.CLIENT)
public final class BossMusicHandler {
    /** Matches the boss's tracking range, so music plays while the boss bar is shown. */
    private static final double RANGE = 80.0;
    private static final int SCAN_INTERVAL = 10;
    /** Music volume while the boss speaks: about -10 dB. */
    private static final float DUCKED = 0.32F;
    /** Silence after the fight before vanilla music may return, in ticks. */
    private static final int QUIET_MIN = 3600;
    private static final int QUIET_MAX = 9600;

    @Nullable
    private static BossMusic music;
    private static boolean fightNearby;
    private static boolean introNearby;
    private static int quietTicks;
    private static final RandomSource RANDOM = RandomSource.create();

    private BossMusicHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            fightNearby = false;
            introNearby = false;
            quietTicks = 0;
            stopMusic(mc);
            return;
        }
        if (quietTicks > 0 && !mc.isPaused()) quietTicks--;
        if (mc.level.getGameTime() % SCAN_INTERVAL != 0) return;

        boolean wasNearby = fightNearby;
        boolean fight = false;
        boolean intro = false;
        for (DarkDoppelgangerEntity doppel : mc.level.getEntitiesOfClass(DarkDoppelgangerEntity.class,
                mc.player.getBoundingBox().inflate(RANGE), DarkDoppelgangerEntity::isAlive)) {
            if (doppel.isInIntro()) intro = true;
            else fight = true;
        }
        fightNearby = fight;
        introNearby = intro;
        if (wasNearby && !fightNearby) {
            stopMusic(mc);
            quietTicks = Mth.nextInt(RANDOM, QUIET_MIN, QUIET_MAX);
        }
        // Silent during the intro. The boss track starts with the fight.
        if (fightNearby && (music == null || !mc.getSoundManager().isActive(music))) {
            music = new BossMusic();
            mc.getSoundManager().play(music);
        }
    }

    private static void stopMusic(Minecraft mc) {
        if (music != null) {
            mc.getSoundManager().stop(music);
            music = null;
        }
    }

    /**
     * No vanilla music while the boss is near, or for a while after the fight. Forge 1.20.1 has no
     * music selection event, so {@code MusicManagerMixin} asks this each tick and, while it holds,
     * stops any vanilla track and starts no new one.
     */
    public static boolean suppressesVanillaMusic() {
        return fightNearby || introNearby || quietTicks > 0;
    }

    private static final class BossMusic extends AbstractTickableSoundInstance {
        BossMusic() {
            super(ModSounds.BOSS_FIGHT_MUSIC.get(), SoundSource.MUSIC, SoundInstance.createUnseededRandom());
            looping = true;
            relative = true;
            attenuation = Attenuation.NONE;
        }

        /** Dips quickly when the boss starts a line and comes back slowly after it. */
        @Override
        public void tick() {
            float target = VoiceCaptions.speaking() ? DUCKED : 1.0F;
            volume += (target - volume) * (target < volume ? 0.4F : 0.08F);
        }
    }
}
