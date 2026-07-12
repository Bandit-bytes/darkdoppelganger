package net.bandit.darkdoppelganger.entity;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.entity.mobs.IAnimatedAttacker;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.goals.SpellBarrageGoal;
import io.redspace.ironsspellbooks.spells.NoneSpell;
import net.bandit.darkdoppelganger.Config;
import net.bandit.darkdoppelganger.DarkDoppelgangerMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class DarkDoppelgangerMinionEntity extends AbstractSpellCastingMob implements IAnimatedAttacker {

    private static final String NBT_SUMMONER_UUID = "SummonerUUID";
    private static final String NBT_IS_BOSS_MINION = "IsBossMinion";
    private static final String NBT_PLAYER_MINION_UUID = "DarkDoppel_SummonerMinionUUID";
    private static final String PERSISTED_TAG = "darkdoppelganger";
    private static final String NBT_WARN_COOLDOWN = "MinionWarnCooldown";

    private static final String NBT_USE_SUMMONER_SKIN = "UseSummonerSkin";
    private static final String NBT_SKIN_PLAYER_UUID = "SkinPlayerUUID";

    private static final int LOADOUT_SYNC_INTERVAL = 20;

    private static final EntityDataAccessor<Boolean> DATA_USE_SUMMONER_SKIN =
            SynchedEntityData.defineId(DarkDoppelgangerMinionEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Optional<UUID>> DATA_SKIN_PLAYER_UUID =
            SynchedEntityData.defineId(DarkDoppelgangerMinionEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    private int age;

    @Nullable
    private UUID summonerUUID;

    private boolean isBossMinion = false;

    public DarkDoppelgangerMinionEntity(EntityType<? extends AbstractSpellCastingMob> type, Level world) {
        super(type, world);
        this.setCustomName(Component.literal("Dark Doppelganger Minion"));
        this.lookControl = this.createLookControl();
        this.moveControl = this.createMoveControl();
        this.setCanPickUpLoot(false);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_USE_SUMMONER_SKIN, false);
        this.entityData.define(DATA_SKIN_PLAYER_UUID, Optional.empty());
    }

    public void setSummonerUUID(UUID uuid) {
        this.summonerUUID = uuid;
    }

    public @Nullable UUID getSummonerUUID() {
        return summonerUUID;
    }

    public void setUseSummonerSkin(boolean value) {
        this.entityData.set(DATA_USE_SUMMONER_SKIN, value);
    }

    public boolean usesSummonerSkin() {
        return this.entityData.get(DATA_USE_SUMMONER_SKIN);
    }

    public void setSkinPlayerUUID(@Nullable UUID uuid) {
        this.entityData.set(DATA_SKIN_PLAYER_UUID, Optional.ofNullable(uuid));
    }

    public @Nullable UUID getSkinPlayerUUID() {
        return this.entityData.get(DATA_SKIN_PLAYER_UUID).orElse(null);
    }

    public void setBossMinion(boolean bossMinion) {
        if (this.isBossMinion == bossMinion) return;
        this.isBossMinion = bossMinion;

        if (!level().isClientSide) {
            applyConfiguredStats();
            this.goalSelector.removeAllGoals(g -> true);
            this.targetSelector.removeAllGoals(g -> true);
            this.registerGoals();
        }
    }

    public boolean isBossMinion() {
        return isBossMinion;
    }

    @Nullable
    public ServerPlayer getSummonerPlayer() {
        if (summonerUUID == null || !(this.level() instanceof ServerLevel serverLevel)) return null;
        return serverLevel.getServer().getPlayerList().getPlayer(summonerUUID);
    }

    @Override
    public boolean isPersistenceRequired() {
        return false;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 200.0)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.ARMOR, 0.0D)
                .add(ForgeMod.ENTITY_REACH.get(), 3.0D);
    }

    private void applyConfiguredStats() {
        if (this.level().isClientSide) return;

        double health = this.isBossMinion ? Config.SPELL_MINION_HEALTH.get() : Config.MINION_HEALTH.get();

        if (this.getAttribute(Attributes.MAX_HEALTH) != null) {
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
        }
        if (this.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(Config.MINION_ATTACK_DAMAGE.get());
        }
        if (this.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(Config.MINION_MOVEMENT_SPEED.get());
        }
        if (this.getAttribute(Attributes.ARMOR) != null) {
            this.getAttribute(Attributes.ARMOR).setBaseValue(Config.MINION_ARMOR.get());
        }

        if (this.getHealth() > this.getMaxHealth() || this.tickCount <= 5) {
            this.setHealth(this.getMaxHealth());
        }
    }

    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        if (!this.level().isClientSide) {
            applyConfiguredStats();
            this.syncCopiedLoadoutFromSummoner();
        }
    }

    private RawAnimation animationToPlay = null;
    private final RawAnimation ANIMATION_SPAWN = RawAnimation.begin().thenPlay("join_1");
    private final AnimationController<DarkDoppelgangerMinionEntity> meleeController =
            new AnimationController<>(this, "keeper_animations", 0, this::predicate);
    private final AnimationController<DarkDoppelgangerMinionEntity> spawnController =
            new AnimationController<>(this, "spawn_animations", 0, this::spawnPredicate);

    @Override
    protected void dropAllDeathLoot(DamageSource source) {
    }

    @Override
    public void playAnimation(String animationId) {
        try {
            animationToPlay = RawAnimation.begin().thenPlay(animationId);
        } catch (Exception ignored) {
            DarkDoppelgangerMod.LOGGER.error("Entity {} Failed to play animation: {}", this, animationId);
        }
    }

    private PlayState predicate(AnimationState<DarkDoppelgangerMinionEntity> animationEvent) {
        var controller = animationEvent.getController();

        if (age > 45 && this.animationToPlay != null) {
            controller.forceAnimationReset();
            controller.setAnimation(animationToPlay);
            animationToPlay = null;
        }
        return spawnController.getAnimationState() == AnimationController.State.STOPPED
                ? PlayState.CONTINUE
                : PlayState.STOP;
    }

    private PlayState spawnPredicate(AnimationState<DarkDoppelgangerMinionEntity> animationEvent) {
        var controller = animationEvent.getController();

        if (age < 45) {
            controller.setAnimation(ANIMATION_SPAWN);
            return PlayState.CONTINUE;
        }
        return PlayState.STOP;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(meleeController);
        controllerRegistrar.add(spawnController);
        super.registerControllers(controllerRegistrar);
    }

    @Override
    public boolean isAnimating() {
        return meleeController.getAnimationState() != AnimationController.State.STOPPED
                || spawnController.getAnimationState() != AnimationController.State.STOPPED
                || super.isAnimating();
    }

    private List<AbstractSpell> getConfiguredSpells(List<? extends String> ids) {
        return ids.stream()
                .map(ResourceLocation::new)
                .map(SpellRegistry::getSpell)
                .filter(spell -> !(spell instanceof NoneSpell))
                .toList();
    }

    private List<AbstractSpell> getMinionConfiguredSpells() {
        return getConfiguredSpells(Config.MINION_SPELLS.get());
    }

    @Nullable
    private AbstractSpell getRandomConfiguredSpell() {
        List<AbstractSpell> spells = getMinionConfiguredSpells();
        if (spells.isEmpty()) return null;
        return spells.get(this.random.nextInt(spells.size()));
    }

    private AbstractSpell getMinionBarrageSpell() {
        try {
            ResourceLocation id = ResourceLocation.tryParse(Config.MINION_BARRAGE_SPELL.get());
            if (id == null) return SpellRegistry.DEVOUR_SPELL.get();

            AbstractSpell spell = SpellRegistry.getSpell(id);
            if (spell instanceof NoneSpell) {
                return SpellRegistry.DEVOUR_SPELL.get();
            }
            return spell;
        } catch (Exception e) {
            return SpellRegistry.DEVOUR_SPELL.get();
        }
    }

    private void syncCopiedLoadoutFromSummoner() {
        if (this.level().isClientSide) return;
        if (this.isBossMinion) return;

        ServerPlayer summoner = getSummonerPlayer();
        if (summoner == null) return;

        DarkDoppelgangerEquipmentHelper.applyPlayerLoadout(this, summoner);

        // Optional:
        // If you want this to feel even more like a pure caster, clear or override mainhand here.
        // this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
    }

    protected MoveControl createMoveControl() {
        return new MoveControl(this) {
            @Override
            protected float rotlerp(float pSourceAngle, float pTargetAngle, float pMaximumChange) {
                double d0 = this.wantedX - this.mob.getX();
                double d1 = this.wantedZ - this.mob.getZ();
                if (d0 * d0 + d1 * d1 < .5f) {
                    return pSourceAngle;
                } else {
                    return super.rotlerp(pSourceAngle, pTargetAngle, pMaximumChange * .25f);
                }
            }
        };
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        if (other == null) return false;
        if (isBossMinion) {
            if (other instanceof DarkDoppelgangerEntity d && !d.isClone) return true;
            if (other instanceof DarkDoppelgangerMinionEntity m && m.isBossMinion()) return true;
            return false;
        }

        ServerPlayer summoner = getSummonerPlayer();
        if (summoner != null) {
            if (other == summoner) return true;
            if (other.isAlliedTo(summoner)) return true;
        }

        return super.isAlliedTo(other);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        if (target == null) return false;
        if (isBossMinion) {
            if (target instanceof DarkDoppelgangerEntity d && !d.isClone) return false;
            if (target instanceof DarkDoppelgangerMinionEntity m && m.isBossMinion()) return false;
            return super.canAttack(target);
        }

        if (target instanceof Player) return false;

        ServerPlayer summoner = getSummonerPlayer();
        if (summoner != null) {
            if (target == summoner) return false;
            if (target.isAlliedTo(summoner)) return false;
        }

        return super.canAttack(target);
    }

    @Override
    public void tick() {
        super.tick();
        age++;

        if (level().isClientSide) return;
        if (isBossMinion) return;

        ServerPlayer sp = getSummonerPlayer();
        if (sp == null || sp.isDeadOrDying()) {
            discard();
            return;
        }

        if (this.tickCount % LOADOUT_SYNC_INTERVAL == 0) {
            this.syncCopiedLoadoutFromSummoner();
        }

        CompoundTag data = sp.getPersistentData().getCompound(PERSISTED_TAG);
        int cd = data.getInt(NBT_WARN_COOLDOWN);
        if (cd > 0) {
            data.putInt(NBT_WARN_COOLDOWN, cd - 1);
            sp.getPersistentData().put(PERSISTED_TAG, data);
        }

        if (sp.level() != this.level()) {
            discard();
            return;
        }

        if (this.getTarget() == null) {
            LivingEntity a = sp.getLastHurtMob();
            LivingEntity b = sp.getLastHurtByMob();

            if (a != null && a.isAlive() && this.canAttack(a)) {
                this.setTarget(a);
            } else if (b != null && b.isAlive() && this.canAttack(b)) {
                this.setTarget(b);
            }
        }

        LivingEntity t = this.getTarget();
        if (t != null && (!t.isAlive() || !this.canAttack(t) || this.distanceToSqr(t) > (40 * 40))) {
            this.setTarget(null);
            t = null;
        }

        // Only follow summoner when not actively fighting.
        if (t == null) {
            double distSqToSummoner = this.distanceToSqr(sp);

            if (distSqToSummoner > 18 * 18) {
                this.teleportTo(sp.getX(), sp.getY(), sp.getZ());
            } else if (distSqToSummoner > 6 * 6) {
                this.getNavigation().moveTo(sp, 1.0D);
            }
        }
    }

    public static boolean playerHasLivingMinion(ServerLevel level, ServerPlayer player) {
        CompoundTag tag = player.getPersistentData();
        if (!tag.hasUUID(NBT_PLAYER_MINION_UUID)) return false;

        UUID id = tag.getUUID(NBT_PLAYER_MINION_UUID);
        Entity e = level.getEntity(id);
        if (e instanceof DarkDoppelgangerMinionEntity m && m.isAlive() && !m.isBossMinion()) {
            return true;
        }

        tag.remove(NBT_PLAYER_MINION_UUID);
        return false;
    }

    public static void linkPlayerToMinion(ServerPlayer player, UUID minionId) {
        player.getPersistentData().putUUID(NBT_PLAYER_MINION_UUID, minionId);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (summonerUUID != null) tag.putUUID(NBT_SUMMONER_UUID, summonerUUID);
        tag.putBoolean(NBT_IS_BOSS_MINION, isBossMinion);
        tag.putBoolean(NBT_USE_SUMMONER_SKIN, this.usesSummonerSkin());

        UUID skinUuid = getSkinPlayerUUID();
        if (skinUuid != null) {
            tag.putUUID(NBT_SKIN_PLAYER_UUID, skinUuid);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        if (tag.hasUUID(NBT_SUMMONER_UUID)) {
            this.summonerUUID = tag.getUUID(NBT_SUMMONER_UUID);
        }

        this.isBossMinion = tag.getBoolean(NBT_IS_BOSS_MINION);
        this.setUseSummonerSkin(tag.getBoolean(NBT_USE_SUMMONER_SKIN));

        if (tag.hasUUID(NBT_SKIN_PLAYER_UUID)) {
            this.setSkinPlayerUUID(tag.getUUID(NBT_SKIN_PLAYER_UUID));
        } else {
            this.setSkinPlayerUUID(null);
        }

        if (!level().isClientSide) {
            applyConfiguredStats();
            this.goalSelector.removeAllGoals(g -> true);
            this.targetSelector.removeAllGoals(g -> true);
            this.registerGoals();
        }
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    protected void registerGoals() {
        setFirstPhaseGoals();

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));

        if (isBossMinion) {
            this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        }
    }

    protected void setFirstPhaseGoals() {
        this.goalSelector.getRunningGoals().forEach(WrappedGoal::stop);
        this.goalSelector.removeAllGoals(x -> true);

        this.goalSelector.addGoal(1, new FloatGoal(this));

        AbstractSpell barrageSpell = getMinionBarrageSpell();
        this.goalSelector.addGoal(2, new SpellBarrageGoal(this, barrageSpell, 4, 8, 35, 70, 1));

        this.goalSelector.addGoal(4, new RangedSpellcasterGoal(this, 0.9D, 6.5F, 8.0F, 14.0F));

        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
    }

    @Override
    public void die(DamageSource source) {
        clearPlayerMinionLink();
        super.die(source);
    }

    @Override
    public void remove(RemovalReason reason) {
        clearPlayerMinionLink();
        super.remove(reason);
    }

    private void clearPlayerMinionLink() {
        if (level().isClientSide) return;
        if (isBossMinion) return;

        ServerPlayer summoner = getSummonerPlayer();
        if (summoner == null) return;

        CompoundTag tag = summoner.getPersistentData();
        if (tag.hasUUID(NBT_PLAYER_MINION_UUID)) {
            UUID stored = tag.getUUID(NBT_PLAYER_MINION_UUID);
            if (stored.equals(this.getUUID())) {
                tag.remove(NBT_PLAYER_MINION_UUID);
            }
        }
    }

    private class RangedSpellcasterGoal extends Goal {
        private final DarkDoppelgangerMinionEntity mob;
        private final double moveSpeed;
        private final float retreatRange;
        private final float castMinRange;
        private final float castMaxRange;
        private int spellCooldown = 0;

        public RangedSpellcasterGoal(DarkDoppelgangerMinionEntity mob, double moveSpeed, float retreatRange, float castMinRange, float castMaxRange) {
            this.mob = mob;
            this.moveSpeed = moveSpeed;
            this.retreatRange = retreatRange;
            this.castMinRange = castMinRange;
            this.castMaxRange = castMaxRange;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = mob.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = mob.getTarget();
            return target != null && target.isAlive() && mob.canAttack(target);
        }

        @Override
        public void stop() {
            mob.getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity target = mob.getTarget();
            if (target == null) return;

            mob.getLookControl().setLookAt(target, 30.0F, 30.0F);

            if (spellCooldown > 0) {
                spellCooldown--;
            }

            if (mob.isCasting()) {
                mob.getNavigation().stop();
                return;
            }

            double distSq = mob.distanceToSqr(target);
            boolean hasLOS = mob.hasLineOfSight(target);

            double retreatSq = retreatRange * retreatRange;
            double minSq = castMinRange * castMinRange;
            double maxSq = castMaxRange * castMaxRange;

            if (distSq < retreatSq) {
                Vec3 away = mob.position().subtract(target.position());
                if (away.lengthSqr() < 0.001D) {
                    away = new Vec3(
                            mob.random.nextDouble() - 0.5D,
                            0.0D,
                            mob.random.nextDouble() - 0.5D
                    );
                }

                away = away.normalize().scale(5.0D);
                Vec3 retreatPos = mob.position().add(away.x, 0.0D, away.z);
                mob.getNavigation().moveTo(retreatPos.x, retreatPos.y, retreatPos.z, 1.15D);
                return;
            }

            if (distSq > maxSq || !hasLOS) {
                mob.getNavigation().moveTo(target, moveSpeed);
                return;
            }

            if (distSq >= minSq && distSq <= maxSq) {
                mob.getNavigation().stop();

                if (spellCooldown <= 0) {
                    AbstractSpell spell = mob.getRandomConfiguredSpell();
                    if (spell != null) {
                        mob.initiateCastSpell(spell, 1);

                        if (!mob.isCasting()) {
                            spellCooldown = 10;
                        } else {
                            spellCooldown = 25 + mob.random.nextInt(15);
                        }
                    }
                }
            }
        }
    }
}