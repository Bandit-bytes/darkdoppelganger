package net.bandit.darkdoppelganger.mixin;

import com.mojang.blaze3d.Blaze3D;
import net.bandit.darkdoppelganger.client.CutsceneClient;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stands in for NeoForge's CalculatePlayerTurnEvent: while the intro cutscene plays, mouse movement
 * is dropped instead of turning the player.
 */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Shadow private double accumulatedDX;
    @Shadow private double accumulatedDY;
    @Shadow private double lastMouseEventTime;

    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
    private void darkdoppelganger$holdDuringCutscene(CallbackInfo ci) {
        if (!CutsceneClient.blocksMouseTurn()) return;
        lastMouseEventTime = Blaze3D.getTime();
        accumulatedDX = 0.0D;
        accumulatedDY = 0.0D;
        ci.cancel();
    }
}
