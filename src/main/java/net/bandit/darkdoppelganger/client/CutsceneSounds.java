package net.bandit.darkdoppelganger.client;

import net.bandit.darkdoppelganger.entity.DarkDoppelgangerEntity;
import net.bandit.darkdoppelganger.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * Wind and a low drone under the intro, made from vanilla sounds. Each viewer's client plays its own.
 * The wind comes in with the letterbox, gusts after the boss rises, drops under its line, gusts through
 * the silences and fades out after the intro. The drone swells while the boss rises and stops on the cut to it.
 */
final class CutsceneSounds {
    private static final int BOSS_SHOT = DarkDoppelgangerEntity.INTRO_BOSS_SHOT;
    private static final int LINE = DarkDoppelgangerEntity.INTRO_LINE;
    private static final int PLAYER_SHOT = DarkDoppelgangerEntity.INTRO_PLAYER_SHOT;
    private static final int BOTH_SHOT = DarkDoppelgangerEntity.INTRO_BOTH_SHOT;
    private static final int END = DarkDoppelgangerEntity.INTRO_TICKS;
    /** Wind volume over the intro, by tick: low under the line's words, which run from LINE + 9 to LINE + 89. */
    private static final int[] WIND_TICKS = {0, 30, 68, 86, BOSS_SHOT, LINE + 6, LINE + 89, PLAYER_SHOT - 4,
            PLAYER_SHOT + 22, BOTH_SHOT - 8, BOTH_SHOT, BOTH_SHOT + 34, END - 20, END};
    private static final float[] WIND_GAIN = {0.0F, 0.5F, 0.55F, 0.85F, 0.55F, 0.28F, 0.28F, 0.7F, 1.0F, 0.75F, 0.95F, 0.8F, 0.75F, 0.45F};
    /** Gusts start at these ticks, over the steady bed. The first starts with the intro. */
    private static final int[] GUST_TICKS = {86, PLAYER_SHOT - 4, BOTH_SHOT};
    private static final int DRONE_START = DarkDoppelgangerEntity.INTRO_EMERGE;
    private static final int DRONE_END = DarkDoppelgangerEntity.INTRO_BOSS_SHOT;
    /**
     * The beacon hum is quiet and the game plays no sound above volume 1, so copies of it start on the
     * same tick and add up.
     */
    private static final int DRONE_LAYERS = 3;
    private static final int FADE_OUT_TICKS = 30;

    private static final List<Layer> PLAYING = new ArrayList<>();
    /** The intro tick the intro ended at, or -1 while it plays. */
    private static int endedAt = -1;

    private CutsceneSounds() {
    }

    static void start() {
        stopAll();
        endedAt = -1;
        play(new Layer(ModSounds.CUTSCENE_WIND_BED.get(), false, 0, true));
        play(new Layer(ModSounds.CUTSCENE_WIND.get(), false, 0, false));
    }

    /** Called once per intro tick, from 1. */
    static void tick(int introTick) {
        if (introTick == DRONE_START) {
            for (int i = 0; i < DRONE_LAYERS; i++) play(new Layer(ModSounds.CUTSCENE_DRONE.get(), true, introTick, false));
        }
        for (int gust : GUST_TICKS) {
            if (introTick == gust) play(new Layer(ModSounds.CUTSCENE_WIND.get(), false, introTick, false));
        }
    }

    /** The wind fades out from here instead of cutting off. */
    static void end(int introTick) {
        if (endedAt < 0) endedAt = introTick;
    }

    private static void stopAll() {
        for (Layer layer : PLAYING) Minecraft.getInstance().getSoundManager().stop(layer);
        PLAYING.clear();
    }

    private static void play(Layer layer) {
        PLAYING.removeIf(Layer::isStopped);
        PLAYING.add(layer);
        Minecraft.getInstance().getSoundManager().play(layer);
    }

    private static float windGain(int tick) {
        if (tick >= WIND_TICKS[WIND_TICKS.length - 1]) return WIND_GAIN[WIND_GAIN.length - 1];
        for (int i = 1; i < WIND_TICKS.length; i++) {
            if (tick < WIND_TICKS[i]) {
                float s = (float) (tick - WIND_TICKS[i - 1]) / (WIND_TICKS[i] - WIND_TICKS[i - 1]);
                return Mth.lerp(s, WIND_GAIN[i - 1], WIND_GAIN[i]);
            }
        }
        return 0.0F;
    }

    private static float droneGain(int tick) {
        float s = Mth.clamp((float) (tick - DRONE_START) / (DRONE_END - DRONE_START - 8), 0.0F, 1.0F);
        // Out over the last three ticks, so it stops cleanly on the cut instead of clicking.
        return (float) Math.pow(s, 2.2) * Mth.clamp((DRONE_END - tick) / 3.0F, 0.0F, 1.0F);
    }

    /** One sound under the intro. It keeps its own clock, so it can fade out after the intro has ended. */
    private static final class Layer extends AbstractTickableSoundInstance {
        private final boolean drone;
        private final int startTick;
        private int age;

        Layer(SoundEvent event, boolean drone, int startTick, boolean loop) {
            super(event, SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
            this.drone = drone;
            this.startTick = startTick;
            looping = loop;
            relative = true;
            attenuation = Attenuation.NONE;
            volume = gain(startTick);
        }

        private float gain(int tick) {
            float gain = drone ? droneGain(tick) : windGain(tick);
            if (endedAt >= 0) gain *= 1.0F - (float) (tick - endedAt) / FADE_OUT_TICKS;
            return Math.max(0.0F, gain);
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            int tick = startTick + ++age;
            boolean over = drone ? tick >= DRONE_END : endedAt >= 0 && tick - endedAt >= FADE_OUT_TICKS;
            if (over) {
                stop();
                return;
            }
            volume = gain(tick);
        }
    }
}
