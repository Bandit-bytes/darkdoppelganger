package net.bandit.darkdoppelganger.mixin;

import net.bandit.darkdoppelganger.client.BossMusicHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.MusicManager;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stands in for NeoForge's SelectMusicEvent with null music: while the boss fight holds vanilla music
 * off, any vanilla track playing is stopped and no new one starts.
 */
@Mixin(MusicManager.class)
public abstract class MusicManagerMixin {
    @Shadow @Final private Minecraft minecraft;
    @Shadow @Nullable private SoundInstance currentMusic;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void darkdoppelganger$holdForBoss(CallbackInfo ci) {
        if (!BossMusicHandler.suppressesVanillaMusic()) return;
        if (currentMusic != null) {
            minecraft.getSoundManager().stop(currentMusic);
            currentMusic = null;
        }
        ci.cancel();
    }
}
