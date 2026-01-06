package net.bandit.darkdoppelganger.entity;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.entity.mobs.IAnimatedAttacker;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.goals.PatrolNearLocationGoal;
import io.redspace.ironsspellbooks.entity.mobs.goals.SpellBarrageGoal;
import io.redspace.ironsspellbooks.entity.mobs.goals.melee.AttackAnimationData;
import io.redspace.ironsspellbooks.spells.NoneSpell;
import io.redspace.ironsspellbooks.entity.mobs.wizards.fire_boss.FireBossMoveControl;
import io.redspace.ironsspellbooks.entity.mobs.wizards.fire_boss.NotIdioticNavigation;
import net.bandit.darkdoppelganger.Config;
import net.bandit.darkdoppelganger.entity.ai.PatchedWarlockAttackGoal;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;


import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class DarkDoppelgangerMinionEntity extends AbstractSpellCastingMob implements Enemy, IAnimatedAttacker {

    private static final String NBT_SUMMONER_UUID = "SummonerUUID";
    private static final String NBT_IS_BOSS_MINION = "IsBossMinion";
    private static final String NBT_PLAYER_MINION_UUID = "DarkDoppel_SummonerMinionUUID";
    private static final String PERSISTED_TAG = "darkdoppelganger";
    private static final String NBT_WARN_COOLDOWN = "MinionWarnCooldown";

    private int age;

    @Nullable
    private UUID summonerUUID;

    private boolean isBossMinion = false;

    public DarkDoppelgangerMinionEntity(EntityType<? extends AbstractSpellCastingMob> type, Level level) {
        super(type, level);
        this.setCustomName(Component.literal("Dark Doppelganger Minion"));
        this.lookControl = this.createLookControl();
        this.moveControl = this.createMoveControl();
    }

    public void setSummonerUUID(@Nullable UUID uuid) {
        this.summonerUUID = uuid;
    }

    public @Nullable UUID getSummonerUUID() {
        return summonerUUID;
    }

    public void setBossMinion(boolean bossMinion) {
        if (this.isBossMinion == bossMinion) return;
        this.isBossMinion = bossMinion;

        if (!level().isClientSide) {
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
        if (summonerUUID == null) return null;
        if (!(this.level() instanceof ServerLevel serverLevel)) return null;
        if (serverLevel.getServer() == null) return null;
        return serverLevel.getServer().getPlayerList().getPlayer(summonerUUID);
    }


    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    public boolean isPersistenceRequired() {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override public void onAddedToLevel() {
        super.onAddedToLevel();
        if (!this.level().isClientSide) {
            var max = this.getAttribute(Attributes.MAX_HEALTH);
            var dmg = this.getAttribute(Attributes.ATTACK_DAMAGE);
            var spd = this.getAttribute(Attributes.MOVEMENT_SPEED);
            var arm = this.getAttribute(Attributes.ARMOR);

            if (max != null) max.setBaseValue(Config.MINION_HEALTH.get());
            if (dmg != null) dmg.setBaseValue(Config.MINION_ATTACK_DAMAGE.get());
            if (spd != null) spd.setBaseValue(Config.MINION_MOVEMENT_SPEED.get());
            if (arm != null) arm.setBaseValue(Config.MINION_ARMOR.get());

            this.setHealth(this.getMaxHealth());
        }
    }

    @Override
    protected void dropAllDeathLoot(ServerLevel p_level, DamageSource damageSource) {
    }

    RawAnimation animationToPlay = null;
    private final RawAnimation ANIMATION_SPAWN = RawAnimation.begin().thenPlay("join_1");
    private final AnimationController<DarkDoppelgangerMinionEntity> meleeController = new AnimationController<>(this, "keeper_animations", 0, this::predicate);
    private final AnimationController<DarkDoppelgangerMinionEntity> spawnController = new AnimationController<>(this, "spawn_animations", 0, this::spawnPredicate);

    @Override
    public void playAnimation(String animationId) {
        try {
            animationToPlay = RawAnimation.begin().thenPlay(animationId);
        } catch (Exception ignored) {
        }
    }

    private PlayState predicate(AnimationState<DarkDoppelgangerMinionEntity> animationEvent) {
        var controller = animationEvent.getController();

        if (age > 45 && this.animationToPlay != null) {
            controller.forceAnimationReset();
            controller.setAnimation(animationToPlay);
            animationToPlay = null;
        }
        return spawnController.getAnimationState() == AnimationController.State.STOPPED ? PlayState.CONTINUE : PlayState.STOP;
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
        return meleeController.getAnimationState() != AnimationController.State.STOPPED || spawnController.getAnimationState() != AnimationController.State.STOPPED || super.isAnimating();
    }

    protected MoveControl createMoveControl() {
        return new FireBossMoveControl(this);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new NotIdioticNavigation(this, level);
    }

    private List<AbstractSpell> getConfiguredSpells(List<? extends String> ids) {
        return ids.stream()
                .map(s -> (s == null) ? null : ResourceLocation.tryParse(s))
                .filter(java.util.Objects::nonNull)
                .map(SpellRegistry::getSpell)
                .filter(spell -> !(spell instanceof NoneSpell))
                .toList();
    }

    private List<AbstractSpell> getMinionConfiguredSpells() {
        return getConfiguredSpells(Config.MINION_SPELLS.get());
    }

    private List<AbstractSpell> getSpellGroup(List<AbstractSpell> list, int start, int count) {
        if (start >= list.size()) return List.of();
        return list.subList(start, Math.min(start + count, list.size()));
    }

    private AbstractSpell getMinionBarrageSpell() {
        try {
            ResourceLocation id = ResourceLocation.tryParse(Config.MINION_BARRAGE_SPELL.get());
            if (id == null) return SpellRegistry.DEVOUR_SPELL.get();

            AbstractSpell spell = SpellRegistry.getSpell(id);
            if (spell instanceof NoneSpell) return SpellRegistry.DEVOUR_SPELL.get();
            return spell;
        } catch (Exception e) {
            return SpellRegistry.DEVOUR_SPELL.get();
        }
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        if (other == null) return false;

        if (isBossMinion) {
            if (other instanceof DarkDoppelgangerEntity d && !d.isClone) return true;
            if (other instanceof DarkDoppelgangerMinionEntity m && m.isBossMinion()) return true;
            return super.isAlliedTo(other);
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
    protected void registerGoals() {
        setFirstPhaseGoals();

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));

        if (isBossMinion) {
            this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        } else {
            this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(
                    this,
                    LivingEntity.class,
                    6,
                    true,
                    true,
                    (e) -> {
                        if (!(e instanceof Mob mob)) return false;
                        if (e instanceof Player) return false;
                        if (mob.getType().getCategory() != MobCategory.MONSTER) return false;

                        ServerPlayer summoner = getSummonerPlayer();
                        if (summoner != null) {
                            if (e == summoner) return false;
                            if (e.isAlliedTo(summoner)) return false;
                        }
                        return this.canAttack(e);
                    }
            ));
        }
    }

    protected void setFirstPhaseGoals() {
        this.goalSelector.removeAllGoals(x -> true);

        this.goalSelector.addGoal(1, new FloatGoal(this));

        AbstractSpell barrageSpell = getMinionBarrageSpell();
        this.goalSelector.addGoal(2, new SpellBarrageGoal(this, barrageSpell, 3, 6, 100, 250, 1));

        var allSpells = new java.util.ArrayList<>(getMinionConfiguredSpells());
        Collections.shuffle(allSpells, new java.util.Random(this.random.nextLong()));

        List<AbstractSpell> group1 = getSpellGroup(allSpells, 0, 3);
        List<AbstractSpell> group2 = getSpellGroup(allSpells, 3, 3);
        List<AbstractSpell> group3 = getSpellGroup(allSpells, 6, 2);
        List<AbstractSpell> group4 = getSpellGroup(allSpells, 8, 4);

        this.goalSelector.addGoal(3, new PatchedWarlockAttackGoal<>(this, 1.25f, 50, 75)
                .setMoveset(List.of(
                        new AttackAnimationData(9, "simple_sword_upward_swipe", 5),
                        new AttackAnimationData(8, "simple_sword_lunge_stab", 6),
                        new AttackAnimationData(10, "simple_sword_stab_alternate", 8),
                        new AttackAnimationData(10, "simple_sword_horizontal_cross_swipe", 8)
                ))
                .setComboChance(.4f)
                .setMeleeAttackInverval(10, 30)
                .setMeleeMovespeedModifier(1.5f)
                .setSpells(group1, group2, group3, group4)
        );

        this.goalSelector.addGoal(4, new PatrolNearLocationGoal(this, 30, .75f));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
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

        double distSq = this.distanceToSqr(sp);
        if (distSq > 18 * 18) {
            this.teleportTo(sp.getX(), sp.getY(), sp.getZ());
        } else if (distSq > 6 * 6) {
            this.getNavigation().moveTo(sp, 1.2);
        }

        if (this.getTarget() == null) {
            LivingEntity a = sp.getLastHurtMob();
            LivingEntity b = sp.getLastHurtByMob();

            if (a != null && a.isAlive() && this.canAttack(a)) this.setTarget(a);
            else if (b != null && b.isAlive() && this.canAttack(b)) this.setTarget(b);
        }

        LivingEntity t = this.getTarget();
        if (t != null && (!t.isAlive() || !this.canAttack(t) || this.distanceToSqr(t) > (32 * 32))) {
            this.setTarget(null);
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
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID(NBT_SUMMONER_UUID)) this.summonerUUID = tag.getUUID(NBT_SUMMONER_UUID);
        this.isBossMinion = tag.getBoolean(NBT_IS_BOSS_MINION);
    }
}
