package net.bandit.darkdoppelganger.entity.ai;

import io.redspace.ironsspellbooks.api.entity.IMagicEntity;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.entity.mobs.IAnimatedAttacker;
import io.redspace.ironsspellbooks.entity.mobs.goals.WarlockAttackGoal;
import io.redspace.ironsspellbooks.entity.mobs.goals.melee.AttackAnimationData;
import io.redspace.ironsspellbooks.entity.mobs.goals.melee.AttackKeyframe;
import io.redspace.ironsspellbooks.network.SyncAnimationPacket;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class PatchedWarlockAttackGoal<T extends PathfinderMob & IAnimatedAttacker & IMagicEntity> extends WarlockAttackGoal {
    public PatchedWarlockAttackGoal(T abstractSpellCastingMob, double pSpeedModifier, int minAttackInterval, int maxAttackInterval) {
        super(abstractSpellCastingMob, pSpeedModifier, minAttackInterval, maxAttackInterval);
        this.wantsToMelee = true;
        this.mob = abstractSpellCastingMob; //shadows super.mob
    }

    protected List<AttackAnimationData> moveList = new ArrayList<>();
    protected final T mob;
    protected int meleeAnimTimer = -1;
    public @Nullable AttackAnimationData currentAttack;
    public @Nullable AttackAnimationData nextAttack;
    public @Nullable AttackAnimationData queueCombo;
    /**
     * chance that, on a successful hit, we skip our attack delay and immediately attack again. chance is doubled against blocking targets
     */
    float comboChance = .3f;

    /** Ticks needed to fully draw a bow before firing. */
    protected int bowDrawTicks = 20;
    protected int bowCooldownMin = 25;
    protected int bowCooldownMax = 40;
    /** -1 while not drawing, otherwise ticks spent drawing so far. */
    protected int bowDrawTime = -1;
    protected int bowCooldown = 0;

    /** True when the mob can shoot and holds a bow or crossbow in its main hand. */
    protected boolean isHoldingBow() {
        return mob instanceof DoppelArcher && mob.getMainHandItem().getItem() instanceof ProjectileWeaponItem;
    }

    @Override
    public void tick() {
        // An archer keeps its distance: disable melee before movement and attack logic run.
        if (isHoldingBow()) {
            wantsToMelee = false;
        } else {
            bowDrawTime = -1;
        }
        super.tick();
    }

    @Override
    public boolean isActing() {
        return super.isActing() || isMeleeing();
    }

    public boolean isMeleeing() {
        return meleeAnimTimer > 0;
    }

    @Override
    public void start() {
        super.start();
        nextAttack = this.getNextAttack(0);
    }

    @Override
    protected void handleAttackLogic(double distanceSquared) {
        if (meleeAnimTimer < 0 && isHoldingBow()) {
            super.handleAttackLogic(distanceSquared); // spellcasting continues alongside the bow
            handleBowLogic(distanceSquared);
            return;
        }
        var meleeRange = meleeRange();
        float rangeMultiplier = nextAttack == null ? 1f : nextAttack.rangeMultiplier;
        float procRangeSqr = meleeRange * meleeRange * rangeMultiplier * rangeMultiplier * 1.2f * 1.2f;
        if (meleeAnimTimer < 0 && (!wantsToMelee || distanceSquared > procRangeSqr || mob.isCasting())) {
            super.handleAttackLogic(distanceSquared);
            return;
        }
        //Handling Animation hit frames
        mob.getLookControl().setLookAt(target);
        if (meleeAnimTimer > 0 && currentAttack != null) {
            //We are currently attacking and are in a melee animation
            forceFaceTarget();
            meleeAnimTimer--;
            if (currentAttack.isHitFrame(meleeAnimTimer)) {
                AttackKeyframe attackData = currentAttack.getHitFrame(meleeAnimTimer);
                onHitFrame(attackData, meleeRange);
            }
            if (currentAttack.canCancel && (currentAttack.isSingleHit() || currentAttack.lengthInTicks - meleeAnimTimer > currentAttack.attacks.keySet().intStream().sorted().findFirst().orElse(0))) {
                Vec3 delta = mob.position().subtract(target.position());
                var modifiedDistanceSquared = delta.x * delta.x + delta.y * delta.y * .5 * .5 + delta.z * delta.z;
                if (modifiedDistanceSquared > meleeRange * meleeRange * 1.5 * 1.5) {
                    stopMeleeAction();
                }
            }
        } else if (queueCombo != null && target != null && !target.isDeadOrDying()) {
            nextAttack = queueCombo;
            queueCombo = null;
            doMeleeAction();
        } else if (meleeAnimTimer == 0) {
            //Reset animations/attack
            nextAttack = getNextAttack((float) distanceSquared);
            resetMeleeAttackInterval(distanceSquared);
            meleeAnimTimer = -1;
        } else {
            //Handling attack delay
            if (distanceSquared < procRangeSqr) {
                if (hasLineOfSight && --this.meleeAttackDelay == 0) {
                    doMeleeAction();
                } else if (this.meleeAttackDelay < 0) {
                    resetMeleeAttackInterval(distanceSquared);
                }
            }
        }
    }

    protected void handleBowLogic(double distanceSquared) {
        if (bowCooldown > 0) bowCooldown--;

        if (mob.isCasting() || target == null || target.isDeadOrDying()) {
            bowDrawTime = -1;
            return;
        }

        float bowRange = spellcastingRange * 1.4f;
        if (bowDrawTime < 0) {
            if (bowCooldown <= 0 && hasLineOfSight && distanceSquared <= bowRange * bowRange) {
                bowDrawTime = 0;
            }
            return;
        }

        mob.getLookControl().setLookAt(target);
        if (!hasLineOfSight) {
            // Hold the draw briefly when the target ducks behind cover, then give up.
            if (seeTime < -20) bowDrawTime = -1;
            return;
        }
        if (++bowDrawTime >= bowDrawTicks) {
            ((DoppelArcher) mob).performBowShot(target);
            bowDrawTime = -1;
            bowCooldown = Mth.randomBetweenInclusive(mob.getRandom(), bowCooldownMin, bowCooldownMax);
        }
    }

    protected void onHitFrame(AttackKeyframe attackKeyframe, float meleeRange) {
        playSwingSound();
        float f = -Utils.getAngle(mob.getX(), mob.getZ(), target.getX(), target.getZ()) - Mth.HALF_PI;
        Vec3 lunge = attackKeyframe.lungeVector().yRot(f);
        doLunge(lunge, meleeRange);

        var forward = mob.getForward();
        // if this is an area attack, collect all nearby like-entities, and evaluate a dot product to determine if our area cone can hit them
        var targets = currentAttack.areaAttackThreshold.isEmpty() ?
                List.of(target) :
                mob.level().getEntitiesOfClass(target.getClass(), mob.getBoundingBox().inflate(spellcastingRange),
                        (entity -> forward.dot(entity.position().subtract(mob.position()).normalize()) >= currentAttack.areaAttackThreshold.get())
                );
        for (LivingEntity target : targets) {
            if (target.distanceToSqr(mob) <= meleeRange * meleeRange && Utils.hasLineOfSight(mob.level(), mob, target, true)) {
                handleDamaging(target, attackKeyframe);
            }
        }
    }

    protected void doLunge(Vec3 vector, float meleeRange) {
        mob.push(vector.x, vector.y, vector.z);
    }

    private void handleDamaging(LivingEntity target, AttackKeyframe attackData) {
        boolean flag = this.mob.doHurtTarget(target);
        target.invulnerableTime = 0;
        float f = -Utils.getAngle(mob.getX(), mob.getZ(), target.getX(), target.getZ()) - Mth.HALF_PI;
        if (flag) {
            if (attackData.extraKnockback() != Vec3.ZERO) {
                target.setDeltaMovement(target.getDeltaMovement().add(attackData.extraKnockback().yRot(f)));
            }
            if (currentAttack.isSingleHit() && ((mob.getRandom().nextFloat() < (comboChance * (target.isBlocking() ? 2 : 1))))) {
                //Attack again! combos!
                queueCombo = getNextAttack(0);
            }
        }
    }

    protected AttackAnimationData getNextAttack(float distanceSquared) {
        //TODO: IAttackAnimationProvider?
        if (this.moveList.isEmpty()) {
            return null;
        } else {
            return moveList.get(mob.getRandom().nextInt(moveList.size()));
        }
    }

    private void forceFaceTarget() {
        double d0 = target.getX() - mob.getX();
        double d1 = target.getZ() - mob.getZ();
        float yRot = (float) (Mth.atan2(d1, d0) * (double) (180F / (float) Math.PI)) - 90.0F;
        mob.setYBodyRot(yRot);
        mob.setYHeadRot(yRot);
        mob.setYRot(yRot);
    }

    public void stopMeleeAction() {
        if (currentAttack != null) {
            meleeAnimTimer = 0;
            PacketDistributor.sendToPlayersTrackingEntity(mob, new SyncAnimationPacket<>("", mob));
        }
    }

    @Override
    protected void doMeleeAction() {
        //anim duration
        currentAttack = nextAttack;
        if (currentAttack != null) {
            this.mob.swing(InteractionHand.MAIN_HAND);
            meleeAnimTimer = currentAttack.lengthInTicks;
            PacketDistributor.sendToPlayersTrackingEntity(mob, new SyncAnimationPacket<>(currentAttack.animationId, mob));
        }
    }

    @Override
    public boolean canContinueToUse() {
        return super.canContinueToUse() || meleeAnimTimer > 0;
    }

    @Override
    public void stop() {
        super.stop();
        this.meleeAnimTimer = -1;
        this.queueCombo = null;
        this.bowDrawTime = -1;
    }

    public void playSwingSound() {
        mob.playSound(SoundRegistry.GENERIC_BLADE_SWING.get(), 1, Mth.randomBetweenInclusive(mob.getRandom(), 12, 18) * .1f);
    }

    public PatchedWarlockAttackGoal<T> setMoveset(List<AttackAnimationData> moveset) {
        this.moveList = moveset;
        nextAttack = getNextAttack(0);
        return this;
    }

    public PatchedWarlockAttackGoal<T> setBowTiming(int drawTicks, int cooldownMin, int cooldownMax) {
        this.bowDrawTicks = drawTicks;
        this.bowCooldownMin = cooldownMin;
        this.bowCooldownMax = cooldownMax;
        return this;
    }

    public PatchedWarlockAttackGoal<T> setComboChance(float comboChance) {
        this.comboChance = comboChance;
        return this;
    }
    @Override
    public float meleeRange() {
        return 4.5f;
    }

}
