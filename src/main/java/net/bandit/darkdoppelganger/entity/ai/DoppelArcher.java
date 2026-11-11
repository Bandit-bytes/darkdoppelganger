package net.bandit.darkdoppelganger.entity.ai;

import net.minecraft.world.entity.LivingEntity;

/**
 * A mob that can fire the bow or crossbow in its main hand. {@link PatchedWarlockAttackGoal}
 * switches to ranged combat when its mob implements this and holds a projectile weapon.
 */
public interface DoppelArcher {
    void performBowShot(LivingEntity target);
}
