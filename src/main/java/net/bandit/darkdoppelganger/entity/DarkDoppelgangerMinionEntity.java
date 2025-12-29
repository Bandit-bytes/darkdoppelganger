package net.bandit.darkdoppelganger.entity;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.entity.mobs.IAnimatedAttacker;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.goals.AttackAnimationData;
import io.redspace.ironsspellbooks.entity.mobs.goals.PatrolNearLocationGoal;
import io.redspace.ironsspellbooks.entity.mobs.goals.SpellBarrageGoal;
import io.redspace.ironsspellbooks.entity.mobs.wizards.GenericAnimatedWarlockAttackGoal;
import net.bandit.darkdoppelganger.Config;
import net.bandit.darkdoppelganger.DarkDoppelgangerMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

import java.util.List;
import java.util.UUID;

public class DarkDoppelgangerMinionEntity extends AbstractSpellCastingMob implements Enemy, IAnimatedAttacker {

    private int age;
    @Nullable private UUID summonerUUID;

    public DarkDoppelgangerMinionEntity(EntityType<? extends AbstractSpellCastingMob> type, Level world) {
        super(type, world);
        this.setCustomName(Component.literal("Dark Doppelganger Minion"));
        this.lookControl = this.createLookControl();
        this.moveControl = this.createMoveControl();
    }

    @Nullable
    public ServerPlayer getSummonerPlayer() {
        if (summonerUUID == null || !(this.level() instanceof ServerLevel serverLevel)) return null;
        return serverLevel.getServer().getPlayerList().getPlayer(summonerUUID);
    }

    public void setSummonerUUID(UUID uuid) {
        this.summonerUUID = uuid;
    }

    @Override
    public boolean isPersistenceRequired() {
        return false;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ARMOR, 4.0);
    }

    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        if (!this.level().isClientSide) {
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(Config.MINION_HEALTH.get());
            this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(Config.MINION_ATTACK_DAMAGE.get());
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(Config.MINION_MOVEMENT_SPEED.get());
            this.getAttribute(Attributes.ARMOR).setBaseValue(Config.MINION_ARMOR.get());
            this.setHealth(this.getMaxHealth());
        }
    }

    RawAnimation animationToPlay = null;
    private final RawAnimation ANIMATION_SPAWN = RawAnimation.begin().thenPlay("join_1");
    private final AnimationController<DarkDoppelgangerMinionEntity> meleeController = new AnimationController<>(this, "keeper_animations", 0, this::predicate);
    private final AnimationController<DarkDoppelgangerMinionEntity> spawnController = new AnimationController<>(this, "spawn_animations", 0, this::spawnPredicate);

    @Override
    protected void dropAllDeathLoot(DamageSource source) {
        // Prevent any default drops
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
    private List<AbstractSpell> getConfiguredSpells(List<? extends String> ids) {
        return ids.stream()
                .map(ResourceLocation::new)
                .map(SpellRegistry::getSpell)
                .filter(spell -> !(spell instanceof io.redspace.ironsspellbooks.spells.NoneSpell))
                .toList();
    }

    protected MoveControl createMoveControl() {
        return new MoveControl(this) {
            //This fixes a bug where a mob tries to path into the block it's already standing, and spins around trying to look "forward"
            //We nullify our rotation calculation if we are close to block we are trying to get to
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
    public void tick() {
        super.tick();
        age++;

        if (level().isClientSide) return;
        Player summoner = getSummonerPlayer();
        if (summoner == null || summoner.isDeadOrDying()) {
            discard();
            return;
        }
        if (summoner.level() != this.level()) {
            discard();
            return;
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (summonerUUID != null) {
            tag.putUUID("SummonerUUID", summonerUUID);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("SummonerUUID")) {
            this.summonerUUID = tag.getUUID("SummonerUUID");
        }
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }
    @Override
    protected void registerGoals() {
        setFirstPhaseGoals();
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    protected void setFirstPhaseGoals() {
        this.goalSelector.getRunningGoals().forEach(WrappedGoal::stop);
        this.goalSelector.removeAllGoals((x) -> true);
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new SpellBarrageGoal(this, SpellRegistry.DEVOUR_SPELL.get(), 3, 6, 100, 250, 1));
        this.goalSelector.addGoal(3, new GenericAnimatedWarlockAttackGoal<>(this, 1.25f, 50, 75, 3f)
                .setMoveset(List.of(
                        new AttackAnimationData(9, "simple_sword_upward_swipe", 5),
                        new AttackAnimationData(8, "simple_sword_lunge_stab", 6),
                        new AttackAnimationData(10, "simple_sword_stab_alternate", 8),
                        new AttackAnimationData(10, "simple_sword_horizontal_cross_swipe", 8)
                ))
                .setComboChance(.4f)
                .setMeleeAttackInverval(10, 30)
                .setMeleeMovespeedModifier(1.5f)
                .setSpells(
                        List.of(SpellRegistry.GUIDING_BOLT_SPELL.get(), SpellRegistry.BLOOD_NEEDLES_SPELL.get(), SpellRegistry.BLOOD_SLASH_SPELL.get()),
                        List.of(SpellRegistry.FANG_WARD_SPELL.get(), SpellRegistry.GUST_SPELL.get()),
                        List.of(SpellRegistry.BURNING_DASH_SPELL.get()),
                        List.of(SpellRegistry.BLIGHT_SPELL.get(), SpellRegistry.INVISIBILITY_SPELL.get())
                )
        );
        this.goalSelector.addGoal(4, new PatrolNearLocationGoal(this, 30, .75f));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
    }
}
