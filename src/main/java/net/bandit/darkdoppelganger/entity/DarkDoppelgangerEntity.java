package net.bandit.darkdoppelganger.entity;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.entity.mobs.IAnimatedAttacker;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.goals.PatrolNearLocationGoal;
import io.redspace.ironsspellbooks.entity.mobs.goals.SpellBarrageGoal;
import io.redspace.ironsspellbooks.entity.mobs.goals.melee.AttackAnimationData;
import io.redspace.ironsspellbooks.entity.mobs.wizards.fire_boss.FireBossMoveControl;
import io.redspace.ironsspellbooks.entity.mobs.wizards.fire_boss.NotIdioticNavigation;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import net.bandit.darkdoppelganger.Config;
import net.bandit.darkdoppelganger.entity.ai.DoppelArcher;
import net.bandit.darkdoppelganger.entity.ai.PatchedWarlockAttackGoal;
import net.bandit.darkdoppelganger.registry.EntityRegistry;
import net.bandit.darkdoppelganger.registry.SoundRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.animation.AnimationState;

import java.util.*;

public class DarkDoppelgangerEntity extends AbstractSpellCastingMob implements Enemy, IAnimatedAttacker, DoppelArcher {

    private static final String NBT_SUMMONER_UUID = "SummonerUUID";
    private static final String NBT_SECOND_PHASE = "SecondPhaseTriggered";
    private static final String NBT_THIRD_PHASE = "ThirdPhaseTriggered";
    private static final String NBT_IS_CLONE = "IsClone";
    private static final String NBT_CLONE_LIFETIME = "CloneLifetime";
    private static final String NBT_MINION_COUNT = "TrackedMinionCount";
    private static final String NBT_IS_ECHO = "IsEchoClone";
    private static final String NBT_CLONE_OWNER = "CloneOwner";
    private static final String NBT_TRANSITION_TICKS = "PhaseTransitionTicks";
    private static final String NBT_SPAWN_TIME = "SpawnTime";

    /** Game time the entity first spawned. Synced so clients only play the spawn animation for a new boss. */
    private static final EntityDataAccessor<Long> DATA_SPAWN_TIME =
            SynchedEntityData.defineId(DarkDoppelgangerEntity.class, EntityDataSerializers.LONG);
    private static final int SPAWN_ANIMATION_TICKS = 45;

    /** Length of the phase change, during which the boss hovers, cannot be hurt and refills its health. */
    public static final int PHASE_TRANSITION_TICKS = 60;
    private static final int TRANSITION_RISE_TICKS = 20;
    private int phaseTransitionTicks = 0;
    private int lastImmuneSoundTick = 0;
    private boolean spawnParticlesShown = false;

    @Nullable
    private UUID summonerUUID;

    private final ServerBossEvent bossEvent;
    private boolean secondPhaseTriggered = false;
    private boolean thirdPhaseTriggered = false;
    public boolean isClone = false;
    private int minionSummonCooldown = 300;
    private int lifeDrainCooldown = 150;
    /** Ticks of warning before a life drain pulse. */
    private static final int LIFE_DRAIN_WINDUP = 20;
    private static final double LIFE_DRAIN_RADIUS = 8.0;
    private int roarSoundCooldown = 800;
    private static final int MAX_MINIONS = 5;
    private int laughCooldown = 0;
    private static final int LAUGH_COOLDOWN_TICKS = 240;
    /** Where the boss last stood on solid ground. Used when it falls out of the world with no player to return to. */
    @Nullable
    private Vec3 lastGroundPos;
    private int teleportCooldown = 0;
    private final Set<UUID> activeMinionUUIDs = new HashSet<>();


    private boolean loadedFromSave = false;
    private int mirrorTheftCooldown = 280;
    private int shadowstepCooldown = 180;
    /** Counts down the warning before a shadowstep; 0 when no shadowstep is pending. */
    private int shadowstepWindup = 0;
    /** The current phase's signature barrage; finishing it leaves the boss briefly exposed. */
    @Nullable
    private AbstractSpell signatureSpell = null;
    private boolean castingSignature = false;
    /** Ticks left in the exposed window after a signature spell; the boss takes extra damage. */
    private int exposedTicks = 0;
    private static final int EXPOSED_DURATION = 60;
    private static final float EXPOSED_DAMAGE_BONUS = 1.25F;
    /**
     * Minecraft caps max health (1024 in vanilla). When the configured health is higher, the boss keeps
     * the capped max health and takes proportionally less damage, so the fight lasts as long as the
     * configured value. 1.0 when no scaling is needed.
     */
    private float damageScale = 1.0F;
    private static final int SHADOWSTEP_WINDUP_TICKS = 25;
    private int mirrorCloneCooldown = 360;
    private int echoCooldown = 260;
    private int equipmentMirrorCooldown = 40;
    private int rescueTeleportCooldown = 0;
    private int strandedBelowTicks = 0;
    private String lastDamageSignature = "";
    private int repeatedDamageCount = 0;
    private String adaptationSignature = "";
    private int adaptationTicks = 0;

    private int cloneLifetime = 0;
    /** The boss that spawned this clone. Clones break when it dies. */
    @Nullable
    private UUID cloneOwnerUUID;
    private boolean isEchoClone = false;
    private List<Vec3> echoPath = List.of();
    private int echoPathIndex = 0;
    private int echoStepCooldown = 0;
    private final Set<UUID> echoHitPlayers = new HashSet<>();

    private final Deque<Vec3> targetMovementHistory = new ArrayDeque<>();
    /** Player whose path is in targetMovementHistory, so an echo never mixes two players' paths. */
    @Nullable
    private UUID historyTargetUUID;



    public DarkDoppelgangerEntity(EntityType<? extends AbstractSpellCastingMob> type, Level world) {
        super(type, world);
        this.setCustomName(Component.literal("Dark Doppelganger"));
        this.bossEvent = new ServerBossEvent(Component.literal("Dark Doppelganger"), ServerBossEvent.BossBarColor.PURPLE, ServerBossEvent.BossBarOverlay.PROGRESS);
        this.lookControl = createLookControl();
        this.moveControl = createMoveControl();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        // 0 means long ago, so a boss without a recorded spawn time never replays the animation.
        builder.define(DATA_SPAWN_TIME, 0L);
    }

    private long ticksSinceSpawn() {
        return level().getGameTime() - entityData.get(DATA_SPAWN_TIME);
    }

    public boolean isPhaseTransitioning() {
        return phaseTransitionTicks > 0;
    }

    /** Skips the AI step (goals, navigation, look control) during a phase change. Gravity and motion still run. */
    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || phaseTransitionTicks > 0;
    }

    protected LookControl createLookControl() {
        return new LookControl(this) {
            @Override
            protected float rotateTowards(float pFrom, float pTo, float pMaxDelta) {
                return super.rotateTowards(pFrom, pTo, pMaxDelta * 2.5f);
            }

            @Override
            protected boolean resetXRotOnTick() {
                return getTarget() == null;
            }
        };
    }

    protected MoveControl createMoveControl() {
        return new FireBossMoveControl(this);
    }

    @Override
    public FireBossMoveControl getMoveControl() {
        return (FireBossMoveControl) super.getMoveControl();
    }

    @Override
    protected PathNavigation createNavigation(Level pLevel) {
        return new NotIdioticNavigation(this, pLevel);
    }

    public void setSummonerPlayer(Player summoner) {
        this.summonerUUID = summoner == null ? null : summoner.getUUID();
        if (summoner != null) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (slot == EquipmentSlot.OFFHAND) {
                    continue;
                }
                ItemStack itemStack = summoner.getItemBySlot(slot);
                if (!itemStack.isEmpty()) {
                    this.setItemSlot(slot, itemStack.copy());
                }
            }
            this.setPersistenceRequired();
        }

        copyAttribute(AttributeRegistry.HOLY_SPELL_POWER);
        copyAttribute(AttributeRegistry.BLOOD_SPELL_POWER);
        copyAttribute(AttributeRegistry.NATURE_SPELL_POWER);
        copyAttribute(AttributeRegistry.ELDRITCH_SPELL_POWER);
        copyAttribute(AttributeRegistry.FIRE_SPELL_POWER);
        copyAttribute(AttributeRegistry.ICE_SPELL_POWER);
        copyAttribute(AttributeRegistry.LIGHTNING_SPELL_POWER);
        copyAttribute(AttributeRegistry.EVOCATION_SPELL_POWER);
        copyAttribute(AttributeRegistry.ENDER_SPELL_POWER);
        copyAttribute(AttributeRegistry.SPELL_POWER);
        boostSpellPowerFromConfig();


        if (Config.DOPPELGANGER_HARD_MODE.get()) {
            this.getAttribute(AttributeRegistry.HOLY_SPELL_POWER.getDelegate()).setBaseValue(1.3);
            this.getAttribute(AttributeRegistry.FIRE_MAGIC_RESIST.getDelegate()).setBaseValue(1.5f);
            this.getAttribute(AttributeRegistry.BLOOD_MAGIC_RESIST.getDelegate()).setBaseValue(1.5f);
            this.getAttribute(AttributeRegistry.ELDRITCH_MAGIC_RESIST.getDelegate()).setBaseValue(1.4f);
            this.getAttribute(AttributeRegistry.ICE_MAGIC_RESIST.getDelegate()).setBaseValue(1.6f);
            this.getAttribute(AttributeRegistry.LIGHTNING_MAGIC_RESIST.getDelegate()).setBaseValue(1.4f);
            this.getAttribute(AttributeRegistry.EVOCATION_MAGIC_RESIST.getDelegate()).setBaseValue(1.3f);
            this.getAttribute(AttributeRegistry.ENDER_MAGIC_RESIST.getDelegate()).setBaseValue(1.4f);
            this.getAttribute(AttributeRegistry.SPELL_RESIST.getDelegate()).setBaseValue(1.5f);
        }
    }

    private void boostSpellPowerFromConfig() {
        double multiplier = Config.DOPPELGANGER_SPELL_POWER_MULTIPLIER.get();
        if (multiplier <= 0.0 || multiplier == 1.0) {
            return;
        }

        scaleSpellPower(AttributeRegistry.HOLY_SPELL_POWER, multiplier);
        scaleSpellPower(AttributeRegistry.BLOOD_SPELL_POWER, multiplier);
        scaleSpellPower(AttributeRegistry.NATURE_SPELL_POWER, multiplier);
        scaleSpellPower(AttributeRegistry.ELDRITCH_SPELL_POWER, multiplier);
        scaleSpellPower(AttributeRegistry.FIRE_SPELL_POWER, multiplier);
        scaleSpellPower(AttributeRegistry.ICE_SPELL_POWER, multiplier);
        scaleSpellPower(AttributeRegistry.LIGHTNING_SPELL_POWER, multiplier);
        scaleSpellPower(AttributeRegistry.EVOCATION_SPELL_POWER, multiplier);
        scaleSpellPower(AttributeRegistry.ENDER_SPELL_POWER, multiplier);
        scaleSpellPower(AttributeRegistry.SPELL_POWER, multiplier);
    }

    private void scaleSpellPower(Holder<Attribute> attribute, double multiplier) {
        AttributeInstance inst = this.getAttribute(attribute);
        if (inst != null) {
            inst.setBaseValue(inst.getBaseValue() * multiplier);
        }
    }

    @Override
    protected void registerGoals() {
        setFirstPhaseGoals();
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                LivingEntity attacker = mob.getLastHurtByMob();
                return attacker != null && isFightTarget(attacker) && super.canUse();
            }
        }.setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    protected void setFirstPhaseGoals() {
        this.goalSelector.getAvailableGoals().forEach(WrappedGoal::stop);
        this.goalSelector.removeAllGoals((x) -> true);
        this.goalSelector.addGoal(1, new FloatGoal(this));
        signatureSpell = SpellRegistry.MAGIC_ARROW_SPELL.get();
        this.goalSelector.addGoal(2, new SpellBarrageGoal(this, signatureSpell, 3, 5, 120, 260, 1));
        this.goalSelector.addGoal(3, new PatchedWarlockAttackGoal<>(this, 1.25f, 50, 75)
                .setMoveset(List.of(
                        new AttackAnimationData(9, "simple_sword_upward_swipe", 5),
                        new AttackAnimationData(8, "simple_sword_lunge_stab", 6),
                        new AttackAnimationData(10, "simple_sword_stab_alternate", 8),
                        new AttackAnimationData(10, "simple_sword_horizontal_cross_swipe", 8)
                ))
                .setComboChance(.4f)
                .setBowTiming(20, 25, 40)
                .setMeleeAttackInverval(10, 30)
                .setMeleeMovespeedModifier(1.5f)
                .setSpellQuality(0.3f, 0.5f)
                .setSpells(
                        spellsFromConfig(Config.DOPPEL_PHASE1_SPELLS_A.get()),
                        spellsFromConfig(Config.DOPPEL_PHASE1_SPELLS_B.get()),
                        spellsFromConfig(Config.DOPPEL_PHASE1_SPELLS_C.get()),
                        spellsFromConfig(Config.DOPPEL_PHASE1_SPELLS_D.get())
                )

        );
        this.goalSelector.addGoal(4, new PatrolNearLocationGoal(this, 30, .75f));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
    }

    protected void setSecondPhaseGoals() {
        this.goalSelector.getAvailableGoals().forEach(WrappedGoal::stop);
        this.goalSelector.removeAllGoals((x) -> true);
        this.goalSelector.addGoal(1, new FloatGoal(this));
        signatureSpell = SpellRegistry.ARROW_VOLLEY_SPELL.get();
        this.goalSelector.addGoal(2, new SpellBarrageGoal(this, signatureSpell, 3, 5, 120, 240, 1));
        this.goalSelector.addGoal(3, new PatchedWarlockAttackGoal<>(this, 1.25f, 50, 75)
                .setMoveset(List.of(
                        new AttackAnimationData(9, "simple_sword_upward_swipe", 5),
                        new AttackAnimationData(8, "simple_sword_lunge_stab", 6),
                        new AttackAnimationData(10, "simple_sword_stab_alternate", 8),
                        new AttackAnimationData(10, "simple_sword_horizontal_cross_swipe", 8)
                ))
                .setComboChance(.4f)
                .setBowTiming(18, 20, 35)
                .setMeleeAttackInverval(10, 30)
                .setMeleeMovespeedModifier(1.5f)
                .setSpellQuality(0.4f, 0.6f)
                .setSpells(
                        spellsFromConfig(Config.DOPPEL_PHASE2_SPELLS_A.get()),
                        spellsFromConfig(Config.DOPPEL_PHASE2_SPELLS_B.get()),
                        spellsFromConfig(Config.DOPPEL_PHASE2_SPELLS_C.get()),
                        spellsFromConfig(Config.DOPPEL_PHASE2_SPELLS_D.get())
                )

        );
        this.goalSelector.addGoal(4, new PatrolNearLocationGoal(this, 30, .75f));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
    }

    protected void setThirdPhaseGoals() {
        this.goalSelector.getAvailableGoals().forEach(WrappedGoal::stop);
        this.goalSelector.removeAllGoals((x) -> true);
        this.goalSelector.addGoal(1, new FloatGoal(this));
        signatureSpell = SpellRegistry.STARFALL_SPELL.get();
        this.goalSelector.addGoal(2, new SpellBarrageGoal(this, signatureSpell, 4, 6, 140, 260, 1));
        this.goalSelector.addGoal(3, new PatchedWarlockAttackGoal<>(this, 1.25f, 50, 75)
                .setMoveset(List.of(
                        new AttackAnimationData(9, "simple_sword_upward_swipe", 5),
                        new AttackAnimationData(8, "simple_sword_lunge_stab", 6),
                        new AttackAnimationData(10, "simple_sword_stab_alternate", 8),
                        new AttackAnimationData(10, "simple_sword_horizontal_cross_swipe", 8)
                ))
                .setComboChance(.4f)
                .setBowTiming(15, 15, 30)
                .setMeleeAttackInverval(10, 30)
                .setMeleeMovespeedModifier(1.5f)
                .setSpellQuality(0.5f, 0.7f)
                .setSpells(
                        spellsFromConfig(Config.DOPPEL_PHASE3_SPELLS_A.get()),
                        spellsFromConfig(Config.DOPPEL_PHASE3_SPELLS_B.get()),
                        spellsFromConfig(Config.DOPPEL_PHASE3_SPELLS_C.get()),
                        spellsFromConfig(Config.DOPPEL_PHASE3_SPELLS_D.get())
                )

        );
        this.goalSelector.addGoal(4, new PatrolNearLocationGoal(this, 30, .75f));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
    }

    protected void setFinalPhaseGoals() {
        this.goalSelector.getAvailableGoals().forEach(WrappedGoal::stop);
        this.goalSelector.removeAllGoals((x) -> true);
        this.goalSelector.addGoal(1, new FloatGoal(this));
        signatureSpell = SpellRegistry.STARFALL_SPELL.get();
        this.goalSelector.addGoal(2, new SpellBarrageGoal(this, signatureSpell, 6, 8, 100, 180, 1));
        this.goalSelector.addGoal(3, new PatchedWarlockAttackGoal<>(this, 1.4f, 30, 50)
                .setMoveset(List.of(
                        new AttackAnimationData(9, "simple_sword_upward_swipe", 5),
                        new AttackAnimationData(8, "simple_sword_lunge_stab", 6),
                        new AttackAnimationData(10, "simple_sword_stab_alternate", 8),
                        new AttackAnimationData(10, "simple_sword_horizontal_cross_swipe", 8)
                ))
                .setComboChance(.7f)
                .setBowTiming(12, 10, 20)
                .setMeleeAttackInverval(10, 20)
                .setMeleeMovespeedModifier(1.7f)
                .setSpellQuality(0.6f, 0.85f)
                .setSpells(
                        spellsFromConfig(Config.DOPPEL_FINAL_SPELLS_A.get()),
                        spellsFromConfig(Config.DOPPEL_FINAL_SPELLS_B.get()),
                        spellsFromConfig(Config.DOPPEL_FINAL_SPELLS_C.get()),
                        spellsFromConfig(Config.DOPPEL_FINAL_SPELLS_D.get())
                )

        );
        this.goalSelector.addGoal(5, new PatrolNearLocationGoal(this, 30, .75f));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
    }
    private static List<AbstractSpell> spellsFromConfig(List<? extends String> ids) {
        List<AbstractSpell> out = new ArrayList<>();
        if (ids == null) return out;
        for (String s : ids) {
            if (s == null || s.isBlank()) continue;
            ResourceLocation rl = ResourceLocation.tryParse(s);
            if (rl == null) continue;
            AbstractSpell spell = SpellRegistry.getSpell(rl);
            if (spell != null && spell != SpellRegistry.none()) {
                out.add(spell);
            }
        }
        return out;
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        this.setPersistenceRequired();
        if (this.isClone) {
            this.addTag("dark_doppelganger_clone");
        } else {
            this.addTag("dark_doppelganger_boss");
        }

        if (this.level().isClientSide) return;

        if (!loadedFromSave) {
            // Fresh spawn only: chunk loads and players coming back into range must not replay the entrance.
            entityData.set(DATA_SPAWN_TIME, level().getGameTime());
            PortalJoinEntity portal = new PortalJoinEntity(EntityRegistry.PORTAL_JOIN_ENTITY.get(), this.level());
            portal.setPos(this.position());
            portal.setYRot(this.getYRot());
            portal.yRotO = this.getYRot();
            this.level().addFreshEntity(portal);
        }
        if (!this.isClone && !loadedFromSave) {
            adjustAttributesFromConfig();
            playIntro();
        }
        if (!this.isClone) {
            restorePhaseGoals();
        } else if (isEchoClone) {
            this.goalSelector.removeAllGoals(g -> true);
            this.targetSelector.removeAllGoals(g -> true);
        }
    }

    private void copyAttribute(Holder<Attribute> attribute) {
        Player summoner = getSummonerPlayer();
        if (summoner == null) return;
        AttributeInstance sourceAttribute = summoner.getAttribute(attribute);
        AttributeInstance targetAttribute = this.getAttribute(attribute);

        if (sourceAttribute != null && targetAttribute != null) {
            targetAttribute.setBaseValue(sourceAttribute.getBaseValue());

            for (AttributeModifier modifier : targetAttribute.getModifiers()) {
                targetAttribute.removeModifier(modifier);
            }

            for (AttributeModifier modifier : sourceAttribute.getModifiers()) {
                targetAttribute.addPermanentModifier(modifier);
            }
        }
    }
    private static List<AbstractSpell> spellsFromIds(List<? extends String> ids, List<AbstractSpell> fallback) {
        if (ids == null || ids.isEmpty()) return fallback;

        List<AbstractSpell> out = new ArrayList<>();
        for (String s : ids) {
            if (s == null || s.isBlank()) continue;

            try {
                ResourceLocation id = ResourceLocation.parse(s);
                var holder = SpellRegistry.REGISTRY.getHolder(id);
                if (holder.isPresent()) {
                    out.add(holder.get().value());
                }
            } catch (Exception ignored) {}
        }
        return out.isEmpty() ? fallback : out;
    }

    private void adjustAttributesFromConfig() {
        if (Config.DOPPELGANGER_HEALTH != null) {
            double health = Config.DOPPELGANGER_HEALTH.get();
            int players = level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(48.0),
                    p -> p.isAlive() && !p.isSpectator()).size();
            if (players > 1) {
                health *= 1.0 + Config.DOPPELGANGER_HEALTH_PER_EXTRA_PLAYER.get() * (players - 1);
            }
            double cap = Attributes.MAX_HEALTH.value() instanceof RangedAttribute ranged ? ranged.getMaxValue() : 1024.0;
            double maxHealth = Math.min(health, cap);
            damageScale = (float) (maxHealth / health);
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(maxHealth);
        }
        if (Config.DOPPELGANGER_ATTACK_DAMAGE != null) {
            this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(Config.DOPPELGANGER_ATTACK_DAMAGE.get());
        }
        if (Config.DOPPELGANGER_MOVEMENT_SPEED != null) {
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(Config.DOPPELGANGER_MOVEMENT_SPEED.get());
        }
        if (Config.DOPPELGANGER_KNOCKBACK_RESISTANCE != null) {
            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(Config.DOPPELGANGER_KNOCKBACK_RESISTANCE.get());
        }
        if (Config.DOPPELGANGER_ARMOR != null) {
            this.getAttribute(Attributes.ARMOR).setBaseValue(Config.DOPPELGANGER_ARMOR.get());
        }
        if (Config.DOPPELGANGER_FOLLOW_RANGE != null) {
            this.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(Config.DOPPELGANGER_FOLLOW_RANGE.get());
        }
        this.setHealth(this.getMaxHealth());
    }

    private void spawnSummoningParticles() {
        for (int i = 0; i < 20; i++) {
            double xOffset = (this.random.nextDouble() - 0.5) * 2;
            double yOffset = this.random.nextDouble();
            double zOffset = (this.random.nextDouble() - 0.5) * 2;

            this.level().addParticle(ParticleTypes.FLAME,
                    this.getX() + xOffset, this.getY() + yOffset, this.getZ() + zOffset,
                    0, 0, 0);
        }
    }

    /** Title card when the boss first appears. Not replayed when the boss is loaded from a save. */
    private void playIntro() {
        Player summoner = getSummonerPlayer();
        Component subtitle = summoner != null
                ? Component.literal(summoner.getName().getString() + "'s reflection has awakened").withStyle(ChatFormatting.GRAY)
                : Component.literal("Your reflection has awakened").withStyle(ChatFormatting.GRAY);
        showTitleToNearby(Component.literal("DARK DOPPELGANGER").withStyle(ChatFormatting.DARK_PURPLE),
                subtitle, 48.0, 70);
    }

    /**
     * Titles draw at 4x the GUI scale, so at 1080p a title has about 480 GUI pixels of width. Long
     * titles stay unbolded so they fit; "DARK DOPPELGANGER" in bold runs off both edges.
     */
    private void showTitleToNearby(Component title, Component subtitle, double range, int stayTicks) {
        for (ServerPlayer player : level().getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(range))) {
            player.connection.send(new ClientboundSetTitlesAnimationPacket(10, stayTicks, 20));
            player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
            player.connection.send(new ClientboundSetTitleTextPacket(title));
        }
    }

    @Override
    public void stopSeenByPlayer(@NotNull ServerPlayer player) {
        super.stopSeenByPlayer(player);
        if (!this.isClone) {
            this.bossEvent.removePlayer(player);
        }
    }


    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (!spawnParticlesShown && ticksSinceSpawn() < SPAWN_ANIMATION_TICKS) {
                spawnParticlesShown = true;
                spawnSummoningParticles();
            }
            return;
        }

        createOrJoinDoppelTeam();
        if (this.isDeadOrDying()) return;
        if (isClone) {
            tickClone();
            return;
        }
        cleanupMinions();

        if (phaseTransitionTicks > 0) {
            tickPhaseTransition();
            this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
            return;
        }
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());

        if (laughCooldown > 0) {
            laughCooldown--;
        }
        if (rescueTeleportCooldown > 0) rescueTeleportCooldown--;
        // Refreshed once a second rather than every tick, which re-applied the effects' attribute modifiers each tick.
        if (Config.DOPPELGANGER_HARD_MODE.get() && tickCount % 20 == 0) {
            this.addEffect(new MobEffectInstance(MobEffectRegistry.OAKSKIN.getDelegate(), 40, 8, false, false, true));
            this.addEffect(new MobEffectInstance(MobEffectRegistry.CHARGED.getDelegate(), 40, 2, false, false, true));
            this.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0, false, false));
        }
        if (tickCount % 20 == 0 && onGround()) {
            lastGroundPos = position();
        }

        tickEncounterRescue();
        tickDoppelgangerIdentity();
        tickSignatureExposure();

        if (thirdPhaseTriggered) {
            // Waves come faster once the final bar is low.
            if (minionSummonCooldown-- <= 0) {
                summonMinions();
                minionSummonCooldown = getHealth() < getMaxHealth() * 0.4F ? 300 : 500;
            }
            if (lifeDrainCooldown > 0 && lifeDrainCooldown <= LIFE_DRAIN_WINDUP && lifeDrainCooldown % 4 == 0) {
                warnLifeDrain(lifeDrainCooldown == LIFE_DRAIN_WINDUP);
            }
            if (lifeDrainCooldown-- <= 0) {
                lifeDrainAttack();
                lifeDrainCooldown = 150;
            }
        }

        if (roarSoundCooldown > 0) roarSoundCooldown--;
    }
    /** Health the fight actually represents, including damage scaling past the max health cap. */
    public float getEffectiveMaxHealth() {
        return getMaxHealth() / damageScale;
    }

    @Nullable
    public ServerPlayer getSummonerPlayer() {
        if (summonerUUID == null) return null;
        if (!(level() instanceof ServerLevel serverLevel)) return null;
        if (serverLevel.getServer() == null) return null;
        return serverLevel.getServer().getPlayerList().getPlayer(summonerUUID);
    }
    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    private void triggerSecondPhase() {
        secondPhaseTriggered = true;
        beginPhaseTransition();
        bossEvent.setName(Component.literal("Dark Doppelganger - Reflection Broken"));
        mirrorCloneCooldown = 280;
        shadowstepCooldown = 80;
        echoCooldown = 180;

        spawnMirrorClones(3, 260);

        playRoar(1.0F);
        playPhaseShatter(
                Component.literal("REFLECTION BROKEN").withStyle(ChatFormatting.DARK_PURPLE),
                Component.literal("The mirror shatters... and multiplies").withStyle(ChatFormatting.GRAY));
        for (ServerPlayer player : level().getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(50))) {
            // Darkness pulses but leaves the boss visible, unlike Blindness, so ranged players can still aim.
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0));
        }
    }

    /**
     * Starts the phase change: the boss stops, rises a little, cannot be hurt, and its health bar
     * refills over {@link #PHASE_TRANSITION_TICKS}. The title and shatter play at the start, the
     * lightning ring when it ends.
     */
    private void beginPhaseTransition() {
        phaseTransitionTicks = PHASE_TRANSITION_TICKS;
        if (isCasting()) cancelCast();
        if (isUsingItem()) stopUsingItem();
        castingSignature = false;
        shadowstepWindup = 0;
        if (exposedTicks > 0) {
            exposedTicks = 0;
            removeEffect(MobEffects.GLOWING);
            removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        }
        getNavigation().stop();
        setNoGravity(true);
        setDeltaMovement(Vec3.ZERO);
    }

    private void tickPhaseTransition() {
        // Close the remaining gap evenly, so health reaches max on the last tick.
        setHealth(getHealth() + (getMaxHealth() - getHealth()) / phaseTransitionTicks);
        boolean rising = PHASE_TRANSITION_TICKS - phaseTransitionTicks < TRANSITION_RISE_TICKS;
        setDeltaMovement(0.0, rising ? 0.06 : 0.0, 0.0);
        if (tickCount % 2 == 0 && level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1.0, getZ(), 8, 0.7, 1.0, 0.7, 0.06);
        }
        if (--phaseTransitionTicks == 0) {
            setHealth(getMaxHealth());
            setNoGravity(false);
            playPhaseImpact();
        }
    }

    /** Explosion flash and a ring of lightning. The lightning is visual only: no damage, no fire. */
    private void playPhaseImpact() {
        if (!(level() instanceof ServerLevel serverLevel)) return;
        serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 1.0, getZ(), 1, 0, 0, 0, 0);
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 4.0F, 0.8F);
        for (int i = 0; i < 10; i++) {
            double angle = Math.toRadians(i * 36);
            double x = getX() + Math.cos(angle) * 10;
            double z = getZ() + Math.sin(angle) * 10;
            LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level());
            if (lightning != null) {
                lightning.moveTo(x, getY(), z);
                lightning.setVisualOnly(true);
                level().addFreshEntity(lightning);
            }
        }
    }

    /** Volume above 1 widens the range the sound is sent to (16 blocks per point of volume). */
    private void playRoar(float pitch) {
        level().playSound(null, getX(), getY(), getZ(), SoundRegistry.BOSS_ROAR.get(), SoundSource.HOSTILE, 4.0F, pitch);
    }

    private void triggerThirdPhase() {
        thirdPhaseTriggered = true;
        beginPhaseTransition();
        bossEvent.setName(Component.literal("Dark Doppelganger - Shadowfall"));
        mirrorTheftCooldown = 100;
        shadowstepCooldown = 40;
        echoCooldown = 80;

        playRoar(1.0F);
        playPhaseShatter(
                Component.literal("SHADOWFALL").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
                Component.literal("Final Form - prepare yourself").withStyle(ChatFormatting.RED));

        // First wave 10 seconds after the phase change ends.
        minionSummonCooldown = 200;
        lifeDrainCooldown = 200;
    }
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isDeadOrDying() || source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
            return false;
        }
        if (isClone) {
            return super.hurt(source, amount);
        }
        if (phaseTransitionTicks > 0) {
            if (source.getEntity() instanceof Player && tickCount - lastImmuneSoundTick >= 10) {
                lastImmuneSoundTick = tickCount;
                level().playSound(null, getX(), getY(), getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 1.0F, 0.6F);
            }
            return false;
        }

        amount *= damageScale;
        if (exposedTicks > 0) {
            amount *= EXPOSED_DAMAGE_BONUS;
        }

        String damageSignature = source.getMsgId();
        if (secondPhaseTriggered && adaptationTicks > 0 && adaptationSignature.equals(damageSignature)) {
            // Archers and melee fighters have one main damage type and cannot easily switch,
            // so learned weapon attacks are resisted less than learned spell schools.
            if (isWeaponDamage(source)) {
                amount *= thirdPhaseTriggered ? 0.7F : 0.8F;
            } else {
                amount *= thirdPhaseTriggered ? 0.45F : 0.65F;
            }
        }
        learnFromDamage(source, damageSignature);

        float newHealth = this.getHealth() - amount;

        if (!secondPhaseTriggered && newHealth <= this.getMaxHealth() * 0.4f) {
            triggerSecondPhase();
            if (Config.DOPPELGANGER_HARD_MODE.get()) {
                setThirdPhaseGoals();
            } else {
                setSecondPhaseGoals();
            }
            return false;
        }

        if (!thirdPhaseTriggered && newHealth <= this.getMaxHealth() * 0.2f) {
            triggerThirdPhase();
            if (Config.DOPPELGANGER_HARD_MODE.get()) {
                setFinalPhaseGoals();
            } else {
                setThirdPhaseGoals();
            }
            return false;
        }

        Entity attacker = source.getEntity();
        if (attacker instanceof LivingEntity living && attacker != this && isFightTarget(living)) {
            this.setTarget(living);
        }

        return super.hurt(source, amount);
    }
    private void cleanupMinions() {
        if (!(level() instanceof ServerLevel serverLevel)) return;

        activeMinionUUIDs.removeIf(uuid -> {
            Entity e = serverLevel.getEntity(uuid);
            return !(e instanceof DarkDoppelgangerMinionEntity) || !e.isAlive();
        });
    }

    private void summonMinions() {
        if (!(level() instanceof ServerLevel serverLevel)) return;
        if (isClone) return;

        cleanupMinions();
        if (activeMinionUUIDs.size() >= MAX_MINIONS) return;

        int toSpawn = Math.min(2, MAX_MINIONS - activeMinionUUIDs.size());
        LivingEntity bossTarget = getTarget();

        for (int i = 0; i < toSpawn; i++) {
            DarkDoppelgangerMinionEntity minion = EntityRegistry.DARK_DOPPELGANGER_MINION.get().create(serverLevel);
            if (minion == null) continue;

            Vec3 desired = new Vec3(getX() + (random.nextInt(5) - 2), getY(), getZ() + (random.nextInt(5) - 2));
            Vec3 spot = findStandingSpot(serverLevel, minion.getType(), desired);
            if (spot == null) spot = position();

            minion.moveTo(spot.x, spot.y, spot.z, getYRot(), getXRot());

            minion.setSummonerUUID(null);
            minion.setBossMinion(true);
            minion.setBossOwnerUUID(this.getUUID());

            minion.getPersistentData().putBoolean("SpawnWeak", true);

            minion.setCustomName(Component.literal("Doppelganger Minion").withStyle(ChatFormatting.DARK_GRAY));
            minion.setCustomNameVisible(true);

            Team team = getTeam();
            if (team instanceof PlayerTeam playerTeam) {
                serverLevel.getScoreboard().addPlayerToTeam(minion.getScoreboardName(), playerTeam);
            }

            serverLevel.addFreshEntity(minion);
            activeMinionUUIDs.add(minion.getUUID());
            if (bossTarget != null && bossTarget.isAlive()) {
                minion.setTarget(bossTarget);
                minion.setLastHurtByMob(bossTarget);
            }
        }
        if (toSpawn > 0) {
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 2.0F, 0.7F);
        }
    }

    private static final DustParticleOptions LIFE_DRAIN_WARNING_DUST =
            new DustParticleOptions(new Vector3f(0.65F, 0.15F, 0.95F), 1.6F);

    /** The second before the pulse: a sound, then a purple ring redrawn every 4 ticks at the pulse's reach. */
    private void warnLifeDrain(boolean first) {
        if (!(level() instanceof ServerLevel serverLevel)) return;
        if (first) {
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.HOSTILE, 2.0F, 0.6F);
        }
        spawnParticleRing(serverLevel, LIFE_DRAIN_WARNING_DUST, LIFE_DRAIN_RADIUS, 0.0);
    }

    private void lifeDrainAttack() {
        // Damage only: the boss no longer heals from this pulse. Same radius as the warning ring.
        level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(LIFE_DRAIN_RADIUS),
                        player -> player.distanceToSqr(this) <= LIFE_DRAIN_RADIUS * LIFE_DRAIN_RADIUS)
                .forEach(player -> player.hurt(level().damageSources().magic(), 4.0F));

        if (level() instanceof ServerLevel serverLevel) {
            spawnParticleRing(serverLevel, ParticleTypes.SCULK_SOUL, LIFE_DRAIN_RADIUS, 0.08);
        }
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.RESPAWN_ANCHOR_DEPLETE, SoundSource.HOSTILE, 2.0F, 0.7F);

        // Life drain is a major attack, so it may laugh -- but still obeys the global cooldown.
        tryBossLaugh(0.65F, 1.15F, 1.0F);
    }

    private void spawnParticleRing(ServerLevel serverLevel, net.minecraft.core.particles.ParticleOptions particle,
                                   double radius, double speed) {
        int points = 64;
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0 * i / points;
            serverLevel.sendParticles(particle, getX() + Math.cos(angle) * radius, getY() + 0.2,
                    getZ() + Math.sin(angle) * radius, 1, 0.0, 0.1, 0.0, speed);
        }
    }

    /**
     * The Doppelganger never heals: no regeneration, no lifesteal (Devour, enchantments,
     * attributes on copied gear, stolen effects). Phase transitions use setHealth directly
     * and are unaffected.
     */
    @Override
    public void heal(float amount) {
    }

    /**
     * Mirrored armor is for looks and mind games, not defense. Copying a fully geared player's
     * armor (plus Protection) made the boss take under a tenth of their damage, so it uses its
     * own base armor and toughness instead and does not wear down the copies.
     */
    @Override
    protected float getDamageAfterArmorAbsorb(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_ARMOR)) return amount;
        AttributeInstance armor = getAttribute(Attributes.ARMOR);
        AttributeInstance toughness = getAttribute(Attributes.ARMOR_TOUGHNESS);
        return CombatRules.getDamageAfterAbsorb(this, amount, source,
                armor == null ? 0.0F : (float) armor.getBaseValue(),
                toughness == null ? 0.0F : (float) toughness.getBaseValue());
    }

    /** Resistance still applies; Protection enchantments on the copied armor do not. */
    @Override
    protected float getDamageAfterMagicAbsorb(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_EFFECTS)) return amount;
        MobEffectInstance resistance = getEffect(MobEffects.DAMAGE_RESISTANCE);
        if (resistance != null && !source.is(DamageTypeTags.BYPASSES_RESISTANCE)) {
            int reduction = Math.min(25, (resistance.getAmplifier() + 1) * 5);
            amount = Math.max(amount * (25 - reduction) / 25.0F, 0.0F);
        }
        return amount;
    }

    @Override
    public void die(@NotNull DamageSource cause) {
        if (isClone) {
            if (level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.POOF, getX(), getY() + 1.0, getZ(), 20, 0.4, 0.7, 0.4, 0.03);
            }
            discard();
            return;
        }

        // Main boss death logic
        if (!thirdPhaseTriggered) {
            this.setHealth(1.0F);
            return;
        }
        if (level() instanceof ServerLevel serverLevel) {
            Component message = cause.getEntity() instanceof Player killer
                    ? Component.empty()
                            .append(killer.getDisplayName())
                            .append(Component.literal(" has shattered their reflection. The Dark Doppelganger is slain!"))
                    : Component.literal("The Dark Doppelganger has been slain!");
            serverLevel.getServer().getPlayerList().broadcastSystemMessage(message.copy().withStyle(ChatFormatting.LIGHT_PURPLE), false);
            this.level().addFreshEntity(new ExperienceOrb(this.level(), this.getX(), this.getY(), this.getZ(), 2500));

            // Everything the boss made breaks with it, so nothing keeps fighting during the victory.
            shatterOwnedClones(serverLevel);
            playShatterBurst(serverLevel);
            playRoar(0.7F);
            showTitleToNearby(Component.literal("SHATTERED").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                    Component.literal("The Dark Doppelganger is defeated").withStyle(ChatFormatting.GRAY), 64.0, 80);
        }

        this.bossEvent.removeAllPlayers();
        discardOwnedMinions();
        super.die(cause);
    }

    private void shatterOwnedClones(ServerLevel serverLevel) {
        AABB area = getBoundingBox().inflate(128.0);
        for (DarkDoppelgangerEntity clone : serverLevel.getEntitiesOfClass(DarkDoppelgangerEntity.class, area,
                d -> d.isClone && getUUID().equals(d.cloneOwnerUUID))) {
            clone.shatterClone();
        }
    }

    /** Removes a clone with a burst of glass and smoke. */
    private void shatterClone() {
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PURPLE_STAINED_GLASS.defaultBlockState()),
                    getX(), getY() + 1.0, getZ(), 30, 0.3, 0.6, 0.3, 0.2);
            serverLevel.sendParticles(ParticleTypes.POOF, getX(), getY() + 1.0, getZ(), 12, 0.3, 0.6, 0.3, 0.03);
        }
        discard();
    }


    private void restorePhaseGoals() {
        if (thirdPhaseTriggered) {
            if (Config.DOPPELGANGER_HARD_MODE.get()) setFinalPhaseGoals();
            else setThirdPhaseGoals();
        } else if (secondPhaseTriggered) {
            if (Config.DOPPELGANGER_HARD_MODE.get()) setThirdPhaseGoals();
            else setSecondPhaseGoals();
        } else {
            setFirstPhaseGoals();
        }
    }

    private void tickDoppelgangerIdentity() {
        LivingEntity target = getTarget();
        if (!(target instanceof Player player) || !player.isAlive()) return;

        if (!player.getUUID().equals(historyTargetUUID)) {
            historyTargetUUID = player.getUUID();
            targetMovementHistory.clear();
        }
        // Record the player's recent path. 4 ticks/sample gives us a readable replay without an entity every tick.
        if (tickCount % 4 == 0) {
            targetMovementHistory.addLast(player.position());
            while (targetMovementHistory.size() > 35) targetMovementHistory.removeFirst();
        }

        if (adaptationTicks > 0) {
            adaptationTicks--;
            if (adaptationTicks == 0) adaptationSignature = "";
        }

        if (equipmentMirrorCooldown-- <= 0) {
            equipmentMirrorCooldown = thirdPhaseTriggered ? 100 : 40;
            if (!thirdPhaseTriggered) mirrorTargetWeapon(player);
        }

        if (secondPhaseTriggered) {
            if (mirrorTheftCooldown-- <= 0) {
                if (tryMirrorTheft(player)) {
                    mirrorTheftCooldown = thirdPhaseTriggered ? 260 : 420;
                } else {
                    mirrorTheftCooldown = 100;
                }
            }

            if (shadowstepWindup > 0) {
                if (--shadowstepWindup == 0) {
                    shadowstepBehind(player);
                    shadowstepCooldown = thirdPhaseTriggered ? 90 : 160;
                }
            } else if (shadowstepCooldown-- <= 0 && distanceToSqr(player) > 12.0 * 12.0) {
                warnShadowstep(player);
            }

            if (mirrorCloneCooldown-- <= 0) {
                spawnMirrorClones(thirdPhaseTriggered ? 2 : 1, thirdPhaseTriggered ? 180 : 220);
                mirrorCloneCooldown = thirdPhaseTriggered ? 320 : 480;
            }

            if (echoCooldown-- <= 0 && targetMovementHistory.size() >= 12) {
                spawnMovementEcho(player);
                echoCooldown = thirdPhaseTriggered ? 220 : 360;
            }
        }
    }


    /**
     * Brings the boss back when it falls out of the world or is left far below the fight.
     * Runs whatever the boss is targeting, so it still works while it fights a pet or summon.
     */
    private void tickEncounterRescue() {
        if (!(level() instanceof ServerLevel serverLevel) || rescueTeleportCooldown > 0) return;
        Player anchor = findRescueAnchor();

        if (getY() < serverLevel.getMinBuildHeight() - 8) {
            boolean rescued = anchor != null && teleportSafelyNearPlayer(anchor, true);
            if (!rescued && lastGroundPos != null) {
                portalTo(serverLevel, lastGroundPos, anchor);
                rescued = true;
            }
            if (rescued) {
                rescueTeleportCooldown = 100;
                strandedBelowTicks = 0;
            }
            return;
        }
        if (anchor == null) {
            strandedBelowTicks = 0;
            return;
        }

        double verticalGap = anchor.getY() - getY();
        if (verticalGap >= 8.0 && distanceToSqr(anchor) <= 64.0 * 64.0) {
            strandedBelowTicks++;
            if (strandedBelowTicks >= 80) {
                if (teleportSafelyNearPlayer(anchor, false)) {
                    rescueTeleportCooldown = 120;
                    strandedBelowTicks = 0;
                }
            }
        } else {
            strandedBelowTicks = 0;
        }
    }

    /** The targeted player, or else the closest player by horizontal distance (the boss may be far below the world). */
    @Nullable
    private Player findRescueAnchor() {
        if (getTarget() instanceof Player player && player.isAlive() && !player.isSpectator()) return player;
        Player closest = null;
        double closestDist = 96.0 * 96.0;
        for (Player player : level().players()) {
            if (!player.isAlive() || player.isSpectator() || player.isCreative()) continue;
            double dx = player.getX() - getX();
            double dz = player.getZ() - getZ();
            double dist = dx * dx + dz * dz;
            if (dist < closestDist) {
                closest = player;
                closestDist = dist;
            }
        }
        return closest;
    }

    private boolean teleportSafelyNearPlayer(Player player, boolean fromVoid) {
        if (!(level() instanceof ServerLevel serverLevel)) return false;

        Vec3 destination = findSafeSpotNear(serverLevel, player);
        if (destination == null) return false;
        portalTo(serverLevel, destination, player);

        if (fromVoid) {
            player.displayClientMessage(Component.literal("The Dark Doppelganger claws its way back from the void.").withStyle(ChatFormatting.DARK_PURPLE), true);
        } else {
            player.displayClientMessage(Component.literal("Your reflection refuses to be left behind.").withStyle(ChatFormatting.DARK_PURPLE), true);
        }
        return true;
    }

    /** Teleports with portal effects at both ends. */
    private void portalTo(ServerLevel serverLevel, Vec3 destination, @Nullable LivingEntity lookAt) {
        Vec3 oldPos = position();
        PortalLeaveEntity leave = new PortalLeaveEntity(EntityRegistry.PORTAL_LEAVE_ENTITY.get(), level());
        leave.setPos(oldPos);
        level().addFreshEntity(leave);
        serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, oldPos.x, oldPos.y + 1.0, oldPos.z, 30, 0.5, 0.8, 0.5, 0.12);

        teleportTo(destination.x, destination.y, destination.z);
        setDeltaMovement(Vec3.ZERO);
        fallDistance = 0.0F;
        getNavigation().stop();
        if (lookAt != null) getLookControl().setLookAt(lookAt, 360.0F, 360.0F);

        PortalJoinEntity join = new PortalJoinEntity(EntityRegistry.PORTAL_JOIN_ENTITY.get(), level());
        join.setPos(destination);
        level().addFreshEntity(join);
        serverLevel.sendParticles(ParticleTypes.PORTAL, destination.x, destination.y + 1.0, destination.z, 36, 0.5, 0.8, 0.5, 0.15);
        level().playSound(null, BlockPos.containing(destination), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 0.75F);
    }

    /** A spot near the player with ground under it, preferring behind them. */
    @Nullable
    private Vec3 findSafeSpotNear(ServerLevel level, Player player) {
        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0, look.z);
        if (horizontal.lengthSqr() < 0.001) horizontal = new Vec3(0, 0, 1);
        horizontal = horizontal.normalize();
        Vec3 side = new Vec3(-horizontal.z, 0.0, horizontal.x);

        // Prefer behind the player, then either side, then nearby offsets.
        Vec3[] offsets = new Vec3[]{
                horizontal.scale(-3.0),
                side.scale(3.0),
                side.scale(-3.0),
                horizontal.scale(-5.0),
                horizontal.scale(3.0),
                new Vec3(2.0, 0.0, 2.0),
                new Vec3(-2.0, 0.0, 2.0),
                new Vec3(2.0, 0.0, -2.0),
                new Vec3(-2.0, 0.0, -2.0)
        };

        for (Vec3 offset : offsets) {
            Vec3 spot = findStandingSpot(level, getType(), player.position().add(offset));
            if (spot != null) return spot;
        }
        return null;
    }

    /**
     * Checks a few heights around {@code desired} for a spot where an entity of {@code type} fits,
     * is out of liquid, and has ground within a block below. Stops teleports and spawns over the void.
     */
    @Nullable
    private static Vec3 findStandingSpot(ServerLevel level, EntityType<?> type, Vec3 desired) {
        for (int yOffset : new int[]{0, 1, -1, 2, -2}) {
            Vec3 candidate = desired.add(0.0, yOffset, 0.0);
            AABB box = type.getDimensions().makeBoundingBox(candidate);
            if (!level.noCollision(box) || level.containsAnyLiquid(box)) continue;
            if (level.noCollision(box.move(0.0, -1.0, 0.0))) continue;
            return candidate;
        }
        return null;
    }

    private void mirrorTargetWeapon(Player player) {
        if (!Config.DOPPELGANGER_COPY_PLAYER_MAINHAND.get()) return;
        ItemStack held = player.getMainHandItem();
        if (!held.isEmpty()) {
            setItemSlot(EquipmentSlot.MAINHAND, held.copy());
            setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        }
    }

    @Override
    public void performBowShot(LivingEntity target) {
        ItemStack weapon = getMainHandItem();
        if (!(weapon.getItem() instanceof ProjectileWeaponItem weaponItem)) return;

        ItemStack ammo = getProjectile(weapon);
        if (ammo.isEmpty()) ammo = new ItemStack(Items.ARROW);

        // The weapon stack carries its enchantments (Power, Flame, Punch, Piercing...) onto the arrow,
        // and customArrow lets modded bows swap in their own projectile.
        AbstractArrow arrow = ProjectileUtil.getMobArrow(this, ammo, 1.0F, weapon);
        arrow = weaponItem.customArrow(arrow, ammo, weapon);
        double damageMultiplier = Config.DOPPELGANGER_BOW_DAMAGE_MULTIPLIER.get();
        if (isClone) damageMultiplier *= 0.4; // mirror clones are a distraction, not a second boss
        arrow.setBaseDamage(arrow.getBaseDamage() * damageMultiplier);
        arrow.setCritArrow(true);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;

        // A little slower than a player's full draw so arrows can be strafed at range;
        // the gravity lead keeps them accurate at any distance.
        float velocity = 2.6F;
        double dx = target.getX() - getX();
        double dy = target.getY(0.3333333333333333) - arrow.getY();
        double dz = target.getZ() - getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        double gravityLead = horizontal * horizontal * 0.03 / (velocity * velocity);
        arrow.shoot(dx, dy + gravityLead, dz, velocity, 2.0F);

        boolean crossbow = weaponItem instanceof CrossbowItem;
        playSound(crossbow ? SoundEvents.CROSSBOW_SHOOT : SoundEvents.ARROW_SHOOT, 1.0F,
                1.0F / (getRandom().nextFloat() * 0.4F + 0.8F));
        level().addFreshEntity(arrow);
    }

    private static boolean isWeaponDamage(DamageSource source) {
        return source.is(DamageTypeTags.IS_PROJECTILE)
                || source.is(DamageTypes.PLAYER_ATTACK)
                || source.is(DamageTypes.MOB_ATTACK);
    }

    private static String describeDamage(DamageSource source) {
        if (source.is(DamageTypeTags.IS_PROJECTILE)) return "arrows";
        if (source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.MOB_ATTACK)) return "melee strikes";
        String id = source.getMsgId();
        int dot = id.lastIndexOf('.');
        return (dot >= 0 ? id.substring(dot + 1) : id).replace('_', ' ');
    }

    /** Big phase-change moment: title card, glass-shatter burst and a shove away from the boss. */
    private void playPhaseShatter(Component title, Component subtitle) {
        if (!(level() instanceof ServerLevel serverLevel)) return;

        playShatterBurst(serverLevel);
        showTitleToNearby(title, subtitle, 50.0, 50);
        for (ServerPlayer player : level().getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(7.0),
                p -> p.distanceToSqr(this) < 7.0 * 7.0)) {
            Vec3 away = new Vec3(player.getX() - getX(), 0.0, player.getZ() - getZ());
            if (away.lengthSqr() < 1.0E-4) continue;
            // Shortened near a drop, so the shove never throws anyone off an End island.
            double strength = SHOVE_STRENGTH * safeShoveFraction(serverLevel, player, away.normalize());
            if (strength < 0.3) continue;
            player.knockback(strength, -away.x, -away.z);
            player.hurtMarked = true;
        }
    }

    private static final double SHOVE_STRENGTH = 1.6;
    /** Roughly how far a full-strength shove carries a player. */
    private static final int SHOVE_DISTANCE = 10;

    /** Fraction of a full shove that keeps the player over ground: 1 with floor all the way, less before a drop. */
    private static double safeShoveFraction(ServerLevel level, Player player, Vec3 direction) {
        for (int step = 1; step <= SHOVE_DISTANCE; step++) {
            Vec3 pos = player.position().add(direction.scale(step));
            if (!hasFloorBelow(level, BlockPos.containing(pos), 6)) {
                return (step - 1) / (double) SHOVE_DISTANCE;
            }
        }
        return 1.0;
    }

    private static boolean hasFloorBelow(ServerLevel level, BlockPos start, int depth) {
        BlockPos.MutableBlockPos cursor = start.mutable();
        for (int i = 0; i <= depth; i++) {
            if (!level.getBlockState(cursor).getCollisionShape(level, cursor).isEmpty()) return true;
            cursor.move(net.minecraft.core.Direction.DOWN);
        }
        return false;
    }

    private void playShatterBurst(ServerLevel serverLevel) {
        serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PURPLE_STAINED_GLASS.defaultBlockState()),
                getX(), getY() + 1.2, getZ(), 160, 1.2, 1.2, 1.2, 0.4);
        serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1.0, getZ(), 80, 1.0, 1.0, 1.0, 0.2);
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 2.0F, 0.6F);
    }

    /**
     * After the boss finishes its phase signature spell it is exposed for a few seconds:
     * glowing, slowed and taking extra damage. Rewards dodging the big attack in any fight style.
     */
    private void tickSignatureExposure() {
        if (exposedTicks > 0) exposedTicks--;
        if (signatureSpell == null) return;

        if (isCasting()) {
            // Spell-casting mobs keep their own MagicData; the entity attachment is unused for them.
            var casting = getMagicData().getCastingSpell();
            if (casting != null && casting.getSpell() == signatureSpell) castingSignature = true;
        } else if (castingSignature) {
            castingSignature = false;
            exposedTicks = EXPOSED_DURATION;
            addEffect(new MobEffectInstance(MobEffects.GLOWING, EXPOSED_DURATION, 0, false, false));
            addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, EXPOSED_DURATION, 2, false, false));
            if (level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT, getX(), getY() + 1.2, getZ(), 30, 0.5, 0.8, 0.5, 0.2);
            }
            level().playSound(null, getX(), getY(), getZ(), SoundRegistry.BOSS_STUN.get(), SoundSource.HOSTILE, 2.0F, 1.0F);
            for (ServerPlayer player : level().getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(50))) {
                player.displayClientMessage(Component.literal("The Doppelganger is spent - strike now!").withStyle(ChatFormatting.GOLD), true);
            }
        }
    }

    /**
     * Caps copied spell power so a high spell power mage is not punished for their build.
     * Iron's Spellbooks reads spell power through this method, so equipment bonuses are capped too.
     */
    @Override
    public double getAttributeValue(Holder<Attribute> attribute) {
        double value = super.getAttributeValue(attribute);
        if (!isSpellPowerAttribute(attribute)) return value;
        double cap = Config.DOPPELGANGER_MAX_COPIED_SPELL_POWER.get();
        if (cap > 0.0) {
            double multiplier = Config.DOPPELGANGER_SPELL_POWER_MULTIPLIER.get();
            if (multiplier > 0.0) cap *= multiplier;
            if (attribute.is(AttributeRegistry.HOLY_SPELL_POWER) && Config.DOPPELGANGER_HARD_MODE.get()) {
                cap = Math.max(cap, 1.3);
            }
            return Math.min(value, cap);
        }
        return value;
    }

    private static boolean isSpellPowerAttribute(Holder<Attribute> attribute) {
        return attribute.is(AttributeRegistry.SPELL_POWER)
                || attribute.is(AttributeRegistry.HOLY_SPELL_POWER)
                || attribute.is(AttributeRegistry.BLOOD_SPELL_POWER)
                || attribute.is(AttributeRegistry.NATURE_SPELL_POWER)
                || attribute.is(AttributeRegistry.ELDRITCH_SPELL_POWER)
                || attribute.is(AttributeRegistry.FIRE_SPELL_POWER)
                || attribute.is(AttributeRegistry.ICE_SPELL_POWER)
                || attribute.is(AttributeRegistry.LIGHTNING_SPELL_POWER)
                || attribute.is(AttributeRegistry.EVOCATION_SPELL_POWER)
                || attribute.is(AttributeRegistry.ENDER_SPELL_POWER);
    }

    private boolean tryMirrorTheft(Player target) {
        List<Holder<MobEffect>> candidates = new ArrayList<>();
        candidates.add(MobEffects.MOVEMENT_SPEED);
        candidates.add(MobEffects.DAMAGE_BOOST);
        candidates.add(MobEffects.DAMAGE_RESISTANCE);
        candidates.add(MobEffects.DIG_SPEED);
        candidates.add(MobEffectRegistry.HASTENED.getDelegate());
        candidates.add(MobEffectRegistry.ECHOING_STRIKES.getDelegate());
        // No Evasion or Abyssal Shroud: on the boss they dodge hits, which makes archers miss constantly.
        Collections.shuffle(candidates, new Random(random.nextLong()));

        for (Holder<MobEffect> effect : candidates) {
            MobEffectInstance existing = target.getEffect(effect);
            if (existing == null) continue;

            int stolenDuration = Math.max(100, Math.min(existing.getDuration(), 240));
            int amplifier = existing.getAmplifier();
            // Resistance V would make the boss immune; stolen Resistance is capped at II (40%).
            if (effect.is(MobEffects.DAMAGE_RESISTANCE)) amplifier = Math.min(amplifier, 1);
            target.removeEffect(effect);
            this.addEffect(new MobEffectInstance(effect, stolenDuration, amplifier, false, true, true));

            if (level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.WITCH, target.getX(), target.getY() + 1.0, target.getZ(), 28, 0.45, 0.7, 0.45, 0.08);
                serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1.0, getZ(), 24, 0.4, 0.7, 0.4, 0.08);
            }
            target.displayClientMessage(Component.literal("Your reflection steals one of your blessings.").withStyle(ChatFormatting.DARK_PURPLE), true);
            tryBossLaugh(0.50F, 1.0F, 1.05F);
            return true;
        }
        return false;
    }

    /** Telegraphs a shadowstep so ranged players get a moment to turn around. */
    private void warnShadowstep(Player target) {
        shadowstepWindup = SHADOWSTEP_WINDUP_TICKS;
        level().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.ILLUSIONER_PREPARE_MIRROR, SoundSource.HOSTILE, 1.5F, 0.8F);
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1.0, getZ(), 40, 0.4, 0.8, 0.4, 0.1);
        }
        target.displayClientMessage(Component.literal("Your reflection is slipping behind you...").withStyle(ChatFormatting.DARK_PURPLE), true);
    }

    private void shadowstepBehind(Player target) {
        if (!(level() instanceof ServerLevel serverLevel)) return;
        // Behind the player first, but only onto solid ground, so it never steps out over the void.
        Vec3 destination = findSafeSpotNear(serverLevel, target);
        if (destination != null) portalTo(serverLevel, destination, target);
    }

    private void spawnMirrorClones(int count, int lifetime) {
        if (!(level() instanceof ServerLevel serverLevel) || isClone) return;
        Player copiedPlayer = getSummonerPlayer();
        LivingEntity currentTarget = getTarget();

        for (int i = 0; i < count; i++) {
            DarkDoppelgangerEntity clone = EntityRegistry.DARK_DOPPELGANGER.get().create(serverLevel);
            if (clone == null) continue;

            clone.isClone = true;
            clone.cloneOwnerUUID = getUUID();
            clone.cloneLifetime = lifetime + random.nextInt(40);
            clone.summonerUUID = this.summonerUUID;
            if (copiedPlayer != null) clone.setSummonerPlayer(copiedPlayer);
            clone.setCustomName(Component.literal("Reflection").withStyle(ChatFormatting.DARK_GRAY));
            clone.setCustomNameVisible(false);

            for (EquipmentSlot slot : EquipmentSlot.values()) {
                clone.setItemSlot(slot, getItemBySlot(slot).copy());
                clone.setDropChance(slot, 0.0F);
            }

            AttributeInstance hp = clone.getAttribute(Attributes.MAX_HEALTH);
            AttributeInstance dmg = clone.getAttribute(Attributes.ATTACK_DAMAGE);
            if (hp != null) hp.setBaseValue(Math.max(60.0, getMaxHealth() * 0.035));
            if (dmg != null) dmg.setBaseValue(Math.max(6.0, getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.45));
            clone.setHealth(clone.getMaxHealth());

            double angle = (Math.PI * 2.0 * i / Math.max(1, count)) + random.nextDouble() * 0.7;
            double radius = 4.0 + random.nextDouble() * 2.5;
            Vec3 spot = findStandingSpot(serverLevel, clone.getType(),
                    new Vec3(getX() + Math.cos(angle) * radius, getY(), getZ() + Math.sin(angle) * radius));
            if (spot == null) {
                spot = findStandingSpot(serverLevel, clone.getType(),
                        new Vec3(getX() + Math.cos(angle) * 2.0, getY(), getZ() + Math.sin(angle) * 2.0));
            }
            if (spot == null) spot = position();
            clone.moveTo(spot.x, spot.y, spot.z, getYRot(), getXRot());
            serverLevel.addFreshEntity(clone);
            if (currentTarget != null && currentTarget.isAlive()) clone.setTarget(currentTarget);

            serverLevel.sendParticles(ParticleTypes.POOF, clone.getX(), clone.getY() + 1.0, clone.getZ(), 18, 0.35, 0.6, 0.35, 0.03);
        }
    }

    private void spawnMovementEcho(Player target) {
        if (!(level() instanceof ServerLevel serverLevel)) return;

        List<Vec3> history = new ArrayList<>(targetMovementHistory);
        int start = Math.max(0, history.size() - 24);
        history = new ArrayList<>(history.subList(start, history.size()));
        if (history.size() < 8) return;

        DarkDoppelgangerEntity echo = EntityRegistry.DARK_DOPPELGANGER.get().create(serverLevel);
        if (echo == null) return;
        echo.isClone = true;
        echo.cloneOwnerUUID = getUUID();
        echo.isEchoClone = true;
        echo.thirdPhaseTriggered = this.thirdPhaseTriggered;
        echo.cloneLifetime = history.size() * 3 + 20;
        echo.echoPath = history;
        echo.summonerUUID = this.summonerUUID;
        echo.setCustomName(Component.literal("Shadow Echo").withStyle(ChatFormatting.DARK_GRAY));
        echo.setCustomNameVisible(false);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            echo.setItemSlot(slot, target.getItemBySlot(slot).copy());
            echo.setDropChance(slot, 0.0F);
        }
        Vec3 first = history.get(0);
        echo.moveTo(first.x, first.y, first.z, target.getYRot(), target.getXRot());
        serverLevel.addFreshEntity(echo);
        serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, first.x, first.y + 1.0, first.z, 35, 0.4, 0.7, 0.4, 0.12);
        target.displayClientMessage(Component.literal("Your shadow remembers where you ran...").withStyle(ChatFormatting.DARK_GRAY), true);
    }

    private void tickClone() {
        if (!(level() instanceof ServerLevel serverLevel)) return;
        if (cloneLifetime > 0 && --cloneLifetime <= 0) {
            discard();
            return;
        }
        // Catches a boss removed without dying (commands, despawn), which skips the shatter in die().
        if (cloneOwnerUUID != null && tickCount % 20 == 0
                && !(serverLevel.getEntity(cloneOwnerUUID) instanceof DarkDoppelgangerEntity owner && owner.isAlive())) {
            shatterClone();
            return;
        }
        if (!isEchoClone) {
            // Subtle tell: reflections trail smoke, the real boss does not.
            if (tickCount % 4 == 0) {
                serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.0, getZ(), 2, 0.25, 0.5, 0.25, 0.01);
            }
            return;
        }

        if (echoPath.isEmpty() || echoPathIndex >= echoPath.size()) {
            discard();
            return;
        }
        if (echoStepCooldown-- > 0) return;
        echoStepCooldown = 2;

        Vec3 next = echoPath.get(echoPathIndex++);
        teleportTo(next.x, next.y, next.z);
        serverLevel.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.9, getZ(), 6, 0.18, 0.35, 0.18, 0.02);

        for (Player player : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(1.15))) {
            if (echoHitPlayers.add(player.getUUID())) {
                player.hurt(level().damageSources().magic(), thirdPhaseTriggered ? 12.0F : 8.0F);
            }
        }
    }

    private void learnFromDamage(DamageSource source, String signature) {
        if (!(source.getEntity() instanceof Player player)) return;

        if (signature.equals(lastDamageSignature)) repeatedDamageCount++;
        else {
            lastDamageSignature = signature;
            repeatedDamageCount = 1;
        }

        if (!secondPhaseTriggered || repeatedDamageCount < 3) return;
        if (signature.equals(adaptationSignature) && adaptationTicks > 0) return;

        adaptationSignature = signature;
        adaptationTicks = thirdPhaseTriggered ? 220 : 160;
        repeatedDamageCount = 0;
        player.displayClientMessage(Component.literal("The Doppelganger has learned your " + describeDamage(source)
                + (isWeaponDamage(source) ? ". It resists them for a while." : ". Change tactics.")).withStyle(ChatFormatting.RED), true);
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ENCHANT, getX(), getY() + 1.1, getZ(), 30, 0.5, 0.8, 0.5, 0.08);
        }
        tryBossLaugh(0.35F, 0.9F, 0.85F);
    }

    /**
     * Central laugh gate. All routine boss laughs should go through this so
     * several mechanics cannot spam the sound back-to-back.
     *
     * @param chance chance from 0.0-1.0 that an eligible event actually laughs
     */
    private void tryBossLaugh(float chance, float volume, float pitch) {
        if (isClone || level().isClientSide || laughCooldown > 0) return;
        if (random.nextFloat() > chance) return;

        level().playSound(
                null,
                getX(), getY(), getZ(),
                SoundRegistry.BOSS_LAUGH.get(),
                SoundSource.HOSTILE,
                volume,
                pitch
        );
        laughCooldown = LAUGH_COOLDOWN_TICKS;
    }

    private void discardOwnedMinions() {
        if (!(level() instanceof ServerLevel serverLevel)) return;
        for (UUID uuid : new HashSet<>(activeMinionUUIDs)) {
            Entity entity = serverLevel.getEntity(uuid);
            if (entity instanceof DarkDoppelgangerMinionEntity minion && getUUID().equals(minion.getBossOwnerUUID())) {
                serverLevel.sendParticles(ParticleTypes.POOF, minion.getX(), minion.getY() + 1.0, minion.getZ(), 12, 0.3, 0.6, 0.3, 0.03);
                minion.discard();
            }
        }
        activeMinionUUIDs.clear();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (summonerUUID != null) tag.putUUID(NBT_SUMMONER_UUID, summonerUUID);
        tag.putBoolean(NBT_SECOND_PHASE, secondPhaseTriggered);
        tag.putBoolean(NBT_THIRD_PHASE, thirdPhaseTriggered);
        tag.putBoolean(NBT_IS_CLONE, isClone);
        tag.putBoolean(NBT_IS_ECHO, isEchoClone);
        if (cloneOwnerUUID != null) tag.putUUID(NBT_CLONE_OWNER, cloneOwnerUUID);
        tag.putInt(NBT_CLONE_LIFETIME, cloneLifetime);
        tag.putInt("MinionSummonCooldown", minionSummonCooldown);
        tag.putInt("LifeDrainCooldown", lifeDrainCooldown);
        tag.putInt("MirrorTheftCooldown", mirrorTheftCooldown);
        tag.putInt("ShadowstepCooldown", shadowstepCooldown);
        tag.putInt("RescueTeleportCooldown", rescueTeleportCooldown);
        tag.putInt("StrandedBelowTicks", strandedBelowTicks);
        tag.putInt("MirrorCloneCooldown", mirrorCloneCooldown);
        tag.putInt("EchoCooldown", echoCooldown);
        tag.putFloat("DamageScale", damageScale);
        tag.putInt(NBT_TRANSITION_TICKS, phaseTransitionTicks);
        tag.putLong(NBT_SPAWN_TIME, entityData.get(DATA_SPAWN_TIME));
        tag.putInt(NBT_MINION_COUNT, activeMinionUUIDs.size());
        int index = 0;
        for (UUID uuid : activeMinionUUIDs) {
            tag.putUUID("TrackedMinion" + index++, uuid);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        loadedFromSave = true;
        if (tag.hasUUID(NBT_SUMMONER_UUID)) summonerUUID = tag.getUUID(NBT_SUMMONER_UUID);
        secondPhaseTriggered = tag.getBoolean(NBT_SECOND_PHASE);
        thirdPhaseTriggered = tag.getBoolean(NBT_THIRD_PHASE);
        isClone = tag.getBoolean(NBT_IS_CLONE);
        // The echo's path is not saved, so a reloaded echo has an empty path and removes itself on its first tick.
        isEchoClone = tag.getBoolean(NBT_IS_ECHO);
        if (tag.hasUUID(NBT_CLONE_OWNER)) cloneOwnerUUID = tag.getUUID(NBT_CLONE_OWNER);
        cloneLifetime = tag.getInt(NBT_CLONE_LIFETIME);
        if (tag.contains("MinionSummonCooldown")) minionSummonCooldown = tag.getInt("MinionSummonCooldown");
        if (tag.contains("LifeDrainCooldown")) lifeDrainCooldown = tag.getInt("LifeDrainCooldown");
        if (tag.contains("MirrorTheftCooldown")) mirrorTheftCooldown = tag.getInt("MirrorTheftCooldown");
        if (tag.contains("ShadowstepCooldown")) shadowstepCooldown = tag.getInt("ShadowstepCooldown");
        if (tag.contains("RescueTeleportCooldown")) rescueTeleportCooldown = tag.getInt("RescueTeleportCooldown");
        if (tag.contains("StrandedBelowTicks")) strandedBelowTicks = tag.getInt("StrandedBelowTicks");
        if (tag.contains("MirrorCloneCooldown")) mirrorCloneCooldown = tag.getInt("MirrorCloneCooldown");
        if (tag.contains("EchoCooldown")) echoCooldown = tag.getInt("EchoCooldown");
        if (tag.contains("DamageScale")) damageScale = Math.max(0.0001F, tag.getFloat("DamageScale"));
        phaseTransitionTicks = tag.getInt(NBT_TRANSITION_TICKS);
        entityData.set(DATA_SPAWN_TIME, tag.getLong(NBT_SPAWN_TIME));

        activeMinionUUIDs.clear();
        int count = tag.getInt(NBT_MINION_COUNT);
        for (int i = 0; i < count; i++) {
            String key = "TrackedMinion" + i;
            if (tag.hasUUID(key)) activeMinionUUIDs.add(tag.getUUID(key));
        }

        if (thirdPhaseTriggered) bossEvent.setName(Component.literal("Dark Doppelganger - Shadowfall"));
        else if (secondPhaseTriggered) bossEvent.setName(Component.literal("Dark Doppelganger - Reflection Broken"));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 6000.0)
                .add(Attributes.ATTACK_DAMAGE, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.20)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.ARMOR, 20.0)
                .add(Attributes.FOLLOW_RANGE, 64.0);
    }

    RawAnimation animationToPlay = null;
    private final RawAnimation ANIMATION_SPAWN = RawAnimation.begin().thenPlay("join_1");
    private final AnimationController<DarkDoppelgangerEntity> meleeController = new AnimationController<>(this, "keeper_animations", 0, this::predicate);
    private final AnimationController<DarkDoppelgangerEntity> spawnController = new AnimationController<>(this, "spawn_animations", 0, this::spawnPredicate);

    @Override
    public void playAnimation(String animationId) {
        try {
            animationToPlay = RawAnimation.begin().thenPlay(animationId);
        } catch (Exception ignored) {
        }
    }

    private PlayState predicate(AnimationState<DarkDoppelgangerEntity> animationEvent) {
        var controller = animationEvent.getController();

        if (ticksSinceSpawn() > SPAWN_ANIMATION_TICKS && this.animationToPlay != null) {
            controller.forceAnimationReset();
            controller.setAnimation(animationToPlay);
            animationToPlay = null;
        }
        return spawnController.getAnimationState() == AnimationController.State.STOPPED ? PlayState.CONTINUE : PlayState.STOP;
    }

    private PlayState spawnPredicate(AnimationState<DarkDoppelgangerEntity> animationEvent) {
        var controller = animationEvent.getController();

        if (ticksSinceSpawn() < SPAWN_ANIMATION_TICKS) {
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
    // Boss music is played client side by BossMusicHandler while the boss is nearby.
    @Override
    public void startSeenByPlayer(@NotNull ServerPlayer player) {
        super.startSeenByPlayer(player);
        if (!this.isClone) {
            this.bossEvent.addPlayer(player);
        }
    }

    /** Like the Wither: boats and minecarts cannot pick it up and trap it. */
    @Override
    protected boolean canRide(Entity vehicle) {
        return false;
    }

    /**
     * Whether the boss turns on something that hit it: players and their pets or summons only.
     * Otherwise Endermen hit by its area spells pull it off the player and pause the fight's
     * mechanics, which all follow a targeted player.
     */
    private static boolean isFightTarget(LivingEntity entity) {
        if (entity instanceof Player) return true;
        if (entity instanceof OwnableEntity ownable && ownable.getOwner() instanceof Player) return true;
        return entity instanceof IMagicSummon summon && summon.getSummoner() instanceof Player;
    }
    private void createOrJoinDoppelTeam() {
        if (level().isClientSide || getTeam() != null) return;

        var scoreboard = level().getScoreboard();
        String teamName = "dark_doppelganger_team";

        PlayerTeam team = scoreboard.getPlayerTeam(teamName);
        if (team == null) {
            team = scoreboard.addPlayerTeam(teamName);
            team.setAllowFriendlyFire(false);
            team.setSeeFriendlyInvisibles(true);
        }
        scoreboard.addPlayerToTeam(getScoreboardName(), team);
    }
}