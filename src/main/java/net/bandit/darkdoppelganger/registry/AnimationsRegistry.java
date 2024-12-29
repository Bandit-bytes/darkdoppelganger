package net.bandit.darkdoppelganger.registry;

import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import net.bandit.darkdoppelganger.DarkDoppelgangerMod;
import net.minecraft.resources.ResourceLocation;

public class AnimationsRegistry {
    public static ResourceLocation ANIMATION_RESOURCE = ResourceLocation.fromNamespaceAndPath(DarkDoppelgangerMod.MOD_ID, "animation");

    public static final AnimationHolder PLAYER_JOIN = new AnimationHolder("darkdoppelganger:join_1", false, true);
    public static final AnimationHolder PLAYER_LEAVE = new AnimationHolder("darkdoppelganger:leave_1", false, true);
}
