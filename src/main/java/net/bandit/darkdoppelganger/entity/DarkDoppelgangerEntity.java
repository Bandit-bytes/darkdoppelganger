package net.bandit.darkdoppelganger.entity;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.entity.mobs.IAnimatedAttacker;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.goals.PatrolNearLocationGoal;
import io.redspace.ironsspellbooks.entity.mobs.goals.SpellBarrageGoal;
import io.redspace.ironsspellbooks.entity.mobs.goals.melee.AttackAnimationData;
import io.redspace.ironsspellbooks.entity.mobs.wizards.fire_boss.FireBossMoveControl;
import io.redspace.ironsspellbooks.entity.mobs.wizards.fire_boss.NotIdioticNavigation;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import net.bandit.darkdoppelganger.Config;
import net.bandit.darkdoppelganger.entity.ai.PatchedWarlockAttackGoal;
import net.bandit.darkdoppelganger.registry.EntityRegistry;
import net.bandit.darkdoppelganger.registry.SoundRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.animation.AnimationState;

import java.util.*;

public class DarkDoppelgangerEntity extends AbstractSpellCastingMob implements Enemy, IAnimatedAttacker {

    private static final String NBT_SUMMONER_UUID = "SummonerUUID";
    private static final String NBT_SECOND_PHASE = "SecondPhaseTriggered";
    private static final String NBT_THIRD_PHASE = "ThirdPhaseTriggered";
    private static final String NBT_IS_CLONE = "IsClone";
    private static final String NBT_CLONE_LIFETIME = "CloneLifetime";
    private static final String NBT_MINION_COUNT = "TrackedMinionCount";

    @Nullable
    private UUID summonerUUID;

    private final ServerBossEvent bossEvent;
    private boolean secondPhaseTriggered = false;
    private boolean thirdPhaseTriggered = false;
    public boolean isClone = false;
    private boolean musicPlaying = false;
    private int minionSummonCooldown = 300;
    private int lifeDrainCooldown = 150;
    private int roarSoundCooldown = 800;
    private static final int MAX_MINIONS = 5;
    private int laughCooldown = 0;
    private static final int LAUGH_COOLDOWN_TICKS = 240;
    private int age;
    private int musicTimer = 0;
    private static final int MUSIC_DURATION = 6160;
    private boolean hasFallenIntoVoid = false;
    private int teleportCooldown = 0;
    private final Set<UUID> activeMinionUUIDs = new HashSet<>();


    private boolean loadedFromSave = false;
    private int mirrorTheftCooldown = 280;
    private int shadowstepCooldown = 180;
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
    private boolean isEchoClone = false;
    private List<Vec3> echoPath = List.of();
    private int echoPathIndex = 0;
    private int echoStepCooldown = 0;
    private final Set<UUID> echoHitPlayers = new HashSet<>();

    private final Deque<Vec3> targetMovementHistory = new ArrayDeque<>();



    public DarkDoppelgangerEntity(EntityType<? extends AbstractSpellCastingMob> type, Level world) {
        super(type, world);
        this.setCustomName(Component.literal("Dark Doppelganger"));
        this.bossEvent = new ServerBossEvent(Component.literal("Dark Doppelganger"), ServerBossEvent.BossBarColor.PURPLE, ServerBossEvent.BossBarOverlay.PROGRESS);
        this.lookControl = createLookControl();
        this.moveControl = createMoveControl();
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
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    protected void setFirstPhaseGoals() {
        this.goalSelector.getAvailableGoals().forEach(WrappedGoal::stop);
        this.goalSelector.removeAllGoals((x) -> true);
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new SpellBarrageGoal(this, SpellRegistry.DEVOUR_SPELL.get(), 3, 6, 100, 250, 1));
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
        this.goalSelector.addGoal(2, new SpellBarrageGoal(this, SpellRegistry.FIREBALL_SPELL.get(), 3, 5, 100, 250, 1));
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
        this.goalSelector.addGoal(2, new SpellBarrageGoal(this, SpellRegistry.RAY_OF_FROST_SPELL.get(), 3, 5, 100, 250, 1));
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
        this.goalSelector.addGoal(2, new SpellBarrageGoal(this, SpellRegistry.SCULK_TENTACLES_SPELL.get(), 3, 4, 100, 160, 1));
        this.goalSelector.addGoal(3, new PatchedWarlockAttackGoal<>(this, 1.4f, 30, 50)
                .setMoveset(List.of(
                        new AttackAnimationData(9, "simple_sword_upward_swipe", 5),
                        new AttackAnimationData(8, "simple_sword_lunge_stab", 6),
                        new AttackAnimationData(10, "simple_sword_stab_alternate", 8),
                        new AttackAnimationData(10, "simple_sword_horizontal_cross_swipe", 8)
                ))
                .setComboChance(.7f)
                .setMeleeAttackInverval(10, 20)
                .setMeleeMovespeedModifier(1.7f)
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

        if (!this.level().isClientSide) {
            if (!this.isClone && !musicPlaying) {
                playBossMusic();
            }
            if (!this.isClone && !loadedFromSave) {
                adjustAttributesFromConfig();
            }
            if (!this.isClone) {
                restorePhaseGoals();
            } else if (isEchoClone) {
                this.goalSelector.removeAllGoals(g -> true);
                this.targetSelector.removeAllGoals(g -> true);
            }
        } else {
            spawnSummoningParticles();
        }
        PortalJoinEntity portal = new PortalJoinEntity(EntityRegistry.PORTAL_JOIN_ENTITY.get(), this.level());
        portal.setPos(this.position());
        portal.setYRot(this.getYRot());
        portal.yRotO = this.getYRot();
        this.level().addFreshEntity(portal);
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
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(Config.DOPPELGANGER_HEALTH.get());
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

    private void stopMinecraftAmbientMusic() {
        if (!level().isClientSide && level().getServer() != null) {
            for (ServerPlayer player : level().getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(96.0))) {
                player.connection.send(new ClientboundStopSoundPacket(ResourceLocation.fromNamespaceAndPath("minecraft", "music.game"), SoundSource.MUSIC));
                player.connection.send(new ClientboundStopSoundPacket(ResourceLocation.fromNamespaceAndPath("minecraft", "music.creative"), SoundSource.MUSIC));
                player.connection.send(new ClientboundStopSoundPacket(ResourceLocation.fromNamespaceAndPath("minecraft", "music.menu"), SoundSource.MUSIC));
                player.connection.send(new ClientboundStopSoundPacket(ResourceLocation.fromNamespaceAndPath("minecraft", "music.overworld.day"), SoundSource.MUSIC));
                player.connection.send(new ClientboundStopSoundPacket(ResourceLocation.fromNamespaceAndPath("minecraft", "music.overworld.night"), SoundSource.MUSIC));
                player.connection.send(new ClientboundStopSoundPacket(ResourceLocation.fromNamespaceAndPath("minecraft", "music.overworld.hills"), SoundSource.MUSIC));
                player.connection.send(new ClientboundStopSoundPacket(ResourceLocation.fromNamespaceAndPath("minecraft", "music.overworld.water"), SoundSource.MUSIC));
            }
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
        createOrJoinDoppelTeam();
        if (this.isDeadOrDying()) return;
        if (isClone) {
            tickClone();
            return;
        }
        if (!level().isClientSide) {
            cleanupMinions();
        }

        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());

        if (musicPlaying) {
            if (musicTimer > 0) {
                musicTimer--;
            } else {
                // The sound finished naturally. Re-arm playback without spamming stop packets every tick.
                musicPlaying = false;
                playBossMusic();
            }
        } else {
            playBossMusic();
        }
        if (!isClone && laughCooldown > 0) {
            laughCooldown--;
        }
        if (this.getHealth() < this.getMaxHealth() * 0.4 && minionSummonCooldown <= 0) {
            summonMinions();
            minionSummonCooldown = 1000;
        }
        if (rescueTeleportCooldown > 0) rescueTeleportCooldown--;
        if (Config.DOPPELGANGER_HARD_MODE.get()) {
            this.addEffect(new MobEffectInstance(MobEffectRegistry.OAKSKIN.getDelegate(), 10, 8, false, false, true));
            this.addEffect(new MobEffectInstance( MobEffectRegistry.CHARGED.getDelegate(), 10, 2, false, false, true));
            this.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 10, 0, false, false));
        }


        if (!level().isClientSide) {
            tickDoppelgangerIdentity();
        }

        if (thirdPhaseTriggered) {
            if (minionSummonCooldown-- <= 0) {
                summonMinions();
                minionSummonCooldown = 500;
            }
            if (lifeDrainCooldown-- <= 0) {
                lifeDrainAttack();
                lifeDrainCooldown = 150;
            }
        }

        if (roarSoundCooldown > 0) roarSoundCooldown--;

        age++;
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
        setHealth(getMaxHealth());
        bossEvent.setName(Component.literal("Dark Doppelganger - Reflection Broken"));
        mirrorCloneCooldown = 280;
        shadowstepCooldown = 80;
        echoCooldown = 180;

        spawnMirrorClones(3, 260);

        for (ServerPlayer player : level().getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(50))) {
            player.sendSystemMessage(Component.literal("The Dark Doppelganger has entered its Second Phase!").withStyle(ChatFormatting.DARK_PURPLE));
            level().playSound(null, getX(), getY(), getZ(), SoundRegistry.BOSS_ROAR.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
        }

        level().explode(null, getX(), getY(), getZ(), 0.0F, Level.ExplosionInteraction.NONE);
        level().addParticle(ParticleTypes.EXPLOSION_EMITTER, getX(), getY(), getZ(), 0, 0, 0);
        for (int i = 0; i < 10; i++) {
            double angle = Math.toRadians(i * 36);
            double x = getX() + Math.cos(angle) * 10;
            double z = getZ() + Math.sin(angle) * 10;
            LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level());
            if (lightning != null) {
                lightning.moveTo(x, getY(), z);
                level().addFreshEntity(lightning);
            }
        }
    }

    private void triggerThirdPhase() {
        thirdPhaseTriggered = true;
        setHealth(getMaxHealth());
        bossEvent.setName(Component.literal("Dark Doppelganger - Shadowfall"));
        mirrorTheftCooldown = 100;
        shadowstepCooldown = 40;
        echoCooldown = 80;

        for (ServerPlayer player : level().getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(50))) {
            level().playSound(null, getX(), getY(), getZ(), SoundRegistry.BOSS_ROAR.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
            player.displayClientMessage(Component.literal("Final Form! Prepare yourself!").withStyle(ChatFormatting.RED), true);
        }

        for (Player player : level().players()) {
            player.getCooldowns().addCooldown(Items.TOTEM_OF_UNDYING, 60);
        }
        level().explode(null, getX(), getY(), getZ(), 0.0F, Level.ExplosionInteraction.TNT);
        level().addParticle(ParticleTypes.EXPLOSION_EMITTER, getX(), getY(), getZ(), 0, 0, 0);
        for (int i = 0; i < 10; i++) {
            double angle = Math.toRadians(i * 36);
            double x = getX() + Math.cos(angle) * 10;
            double z = getZ() + Math.sin(angle) * 10;
            LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level());
            if (lightning != null) {
                lightning.moveTo(x, getY(), z);
                level().addFreshEntity(lightning);
            }
        }

        minionSummonCooldown = 1050;
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

        String damageSignature = source.getMsgId();
        if (secondPhaseTriggered && adaptationTicks > 0 && adaptationSignature.equals(damageSignature)) {
            amount *= thirdPhaseTriggered ? 0.45F : 0.65F;
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
        if (attacker instanceof LivingEntity && attacker != this) {
            this.setTarget((LivingEntity) attacker);
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
        if (minionSummonCooldown > 0) return;

        cleanupMinions();
        if (activeMinionUUIDs.size() >= MAX_MINIONS) return;

        int toSpawn = Math.min(2, MAX_MINIONS - activeMinionUUIDs.size());
        LivingEntity bossTarget = getTarget();

        for (int i = 0; i < toSpawn; i++) {
            DarkDoppelgangerMinionEntity minion = EntityRegistry.DARK_DOPPELGANGER_MINION.get().create(serverLevel);
            if (minion == null) continue;

            double x = getX() + (random.nextInt(5) - 2);
            double z = getZ() + (random.nextInt(5) - 2);

            minion.moveTo(x, getY(), z, getYRot(), getXRot());

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

        minionSummonCooldown = 500;
    }

    private void lifeDrainAttack() {
        level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(8)).forEach(player -> {
            player.hurt(level().damageSources().magic(), 4.0F);
            heal(4.0F);
        });

        // Life drain is a major attack, so it may laugh -- but still obeys the global cooldown.
        tryBossLaugh(0.65F, 1.15F, 1.0F);
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
        if (musicPlaying) {
            stopBossMusic();
            musicPlaying = false;
            musicTimer = 0;
        }
        if (!this.level().isClientSide) {
            if (cause.getEntity() instanceof ServerPlayer serverPlayer) {
                if (serverPlayer.getServer() != null) {
                    serverPlayer.sendSystemMessage(Component.literal("You have slain the Dark Doppelganger!"));
                }
            }
            this.level().addFreshEntity(new ExperienceOrb(this.level(), this.getX(), this.getY(), this.getZ(), 2500));
        }

        if (this.level().getServer() != null) {
            this.level().getServer().getPlayerList().getPlayers().forEach(player -> {
                if (player.connection != null) {
                    player.connection.send(
                            new ClientboundStopSoundPacket(SoundRegistry.BOSS_FIGHT_MUSIC.get().getLocation(), SoundSource.MUSIC)
                    );
                }
            });
        }
        if (this.bossEvent != null) {
            this.bossEvent.removeAllPlayers();
        }
        discardOwnedMinions();
        super.die(cause);
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

        tickEncounterRescue(player);

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

            if (shadowstepCooldown-- <= 0 && distanceToSqr(player) > 12.0 * 12.0) {
                shadowstepBehind(player);
                shadowstepCooldown = thirdPhaseTriggered ? 90 : 160;
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


    private void tickEncounterRescue(Player player) {
        if (!(level() instanceof ServerLevel serverLevel) || isClone || rescueTeleportCooldown > 0) return;

        if (getY() < serverLevel.getMinBuildHeight() - 8) {
            if (teleportSafelyNearPlayer(player, true)) {
                rescueTeleportCooldown = 100;
                strandedBelowTicks = 0;
            }
            return;
        }

        double verticalGap = player.getY() - getY();
        if (verticalGap >= 8.0 && distanceToSqr(player) <= 64.0 * 64.0) {
            strandedBelowTicks++;
            if (strandedBelowTicks >= 80) {
                if (teleportSafelyNearPlayer(player, false)) {
                    rescueTeleportCooldown = 120;
                    strandedBelowTicks = 0;
                }
            }
        } else {
            strandedBelowTicks = 0;
        }
    }

    private boolean teleportSafelyNearPlayer(Player player, boolean fromVoid) {
        if (!(level() instanceof ServerLevel serverLevel)) return false;

        Vec3 oldPos = position();
        Vec3 destination = findSafeRescuePosition(serverLevel, player);
        if (destination == null) return false;

        PortalLeaveEntity leave = new PortalLeaveEntity(EntityRegistry.PORTAL_LEAVE_ENTITY.get(), level());
        leave.setPos(oldPos);
        level().addFreshEntity(leave);
        serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, oldPos.x, oldPos.y + 1.0, oldPos.z, 30, 0.5, 0.8, 0.5, 0.12);

        teleportTo(destination.x, destination.y, destination.z);
        setDeltaMovement(Vec3.ZERO);
        fallDistance = 0.0F;
        getNavigation().stop();
        getLookControl().setLookAt(player, 360.0F, 360.0F);

        PortalJoinEntity join = new PortalJoinEntity(EntityRegistry.PORTAL_JOIN_ENTITY.get(), level());
        join.setPos(destination);
        level().addFreshEntity(join);
        serverLevel.sendParticles(ParticleTypes.PORTAL, destination.x, destination.y + 1.0, destination.z, 36, 0.5, 0.8, 0.5, 0.15);
        level().playSound(null, BlockPos.containing(destination), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 0.75F);

        if (fromVoid) {
            player.displayClientMessage(Component.literal("The Dark Doppelganger claws its way back from the void.").withStyle(ChatFormatting.DARK_PURPLE), true);
        } else {
            player.displayClientMessage(Component.literal("Your reflection refuses to be left behind.").withStyle(ChatFormatting.DARK_PURPLE), true);
        }
        return true;
    }

    @Nullable
    private Vec3 findSafeRescuePosition(ServerLevel level, Player player) {
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
            for (int yOffset : new int[]{0, 1, -1, 2, -2}) {
                Vec3 candidate = player.position().add(offset).add(0.0, yOffset, 0.0);
                BlockPos feet = BlockPos.containing(candidate);
                BlockPos floor = feet.below();

                if (level.getBlockState(floor).getCollisionShape(level, floor).isEmpty()) continue;

                double dx = candidate.x - getX();
                double dy = candidate.y - getY();
                double dz = candidate.z - getZ();
                if (level.noCollision(this, getBoundingBox().move(dx, dy, dz))) {
                    return new Vec3(candidate.x, feet.getY(), candidate.z);
                }
            }
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

    private boolean tryMirrorTheft(Player target) {
        List<Holder<MobEffect>> candidates = new ArrayList<>();
        candidates.add(MobEffects.MOVEMENT_SPEED);
        candidates.add(MobEffects.DAMAGE_BOOST);
        candidates.add(MobEffects.DAMAGE_RESISTANCE);
        candidates.add(MobEffects.REGENERATION);
        candidates.add(MobEffects.DIG_SPEED);
        candidates.add(MobEffectRegistry.HASTENED.getDelegate());
        candidates.add(MobEffectRegistry.EVASION.getDelegate());
        candidates.add(MobEffectRegistry.ECHOING_STRIKES.getDelegate());
        candidates.add(MobEffectRegistry.ABYSSAL_SHROUD.getDelegate());
        Collections.shuffle(candidates, new Random(random.nextLong()));

        for (Holder<MobEffect> effect : candidates) {
            MobEffectInstance existing = target.getEffect(effect);
            if (existing == null) continue;

            int stolenDuration = Math.max(100, Math.min(existing.getDuration(), 240));
            int amplifier = existing.getAmplifier();
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

    private void shadowstepBehind(Player target) {
        if (!(level() instanceof ServerLevel serverLevel)) return;

        Vec3 oldPos = position();
        Vec3 look = target.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0, look.z);
        if (horizontal.lengthSqr() < 0.001) horizontal = new Vec3(0, 0, 1);
        horizontal = horizontal.normalize();
        Vec3 desired = target.position().subtract(horizontal.scale(3.0));

        Vec3 destination = null;
        for (double yOffset : new double[]{0, 1, -1, 2}) {
            Vec3 candidate = new Vec3(desired.x, target.getY() + yOffset, desired.z);
            double dx = candidate.x - getX();
            double dy = candidate.y - getY();
            double dz = candidate.z - getZ();
            if (serverLevel.noCollision(this, getBoundingBox().move(dx, dy, dz))) {
                destination = candidate;
                break;
            }
        }
        if (destination == null) return;

        PortalLeaveEntity leave = new PortalLeaveEntity(EntityRegistry.PORTAL_LEAVE_ENTITY.get(), level());
        leave.setPos(oldPos);
        level().addFreshEntity(leave);
        serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, oldPos.x, oldPos.y + 1.0, oldPos.z, 28, 0.45, 0.7, 0.45, 0.12);

        teleportTo(destination.x, destination.y, destination.z);
        getLookControl().setLookAt(target, 360.0F, 360.0F);

        PortalJoinEntity join = new PortalJoinEntity(EntityRegistry.PORTAL_JOIN_ENTITY.get(), level());
        join.setPos(destination);
        level().addFreshEntity(join);
        serverLevel.sendParticles(ParticleTypes.PORTAL, destination.x, destination.y + 1.0, destination.z, 32, 0.45, 0.7, 0.45, 0.15);
        level().playSound(null, blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 0.75F);
    }

    private void spawnMirrorClones(int count, int lifetime) {
        if (!(level() instanceof ServerLevel serverLevel) || isClone) return;
        Player copiedPlayer = getSummonerPlayer();
        LivingEntity currentTarget = getTarget();

        for (int i = 0; i < count; i++) {
            DarkDoppelgangerEntity clone = EntityRegistry.DARK_DOPPELGANGER.get().create(serverLevel);
            if (clone == null) continue;

            clone.isClone = true;
            clone.cloneLifetime = lifetime + random.nextInt(40);
            clone.summonerUUID = this.summonerUUID;
            if (copiedPlayer != null) clone.setSummonerPlayer(copiedPlayer);
            clone.setCustomName(getCustomName());
            clone.setCustomNameVisible(isCustomNameVisible());

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
            clone.moveTo(getX() + Math.cos(angle) * radius, getY(), getZ() + Math.sin(angle) * radius, getYRot(), getXRot());
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
        if (level().isClientSide) return;
        if (cloneLifetime > 0 && --cloneLifetime <= 0) {
            discard();
            return;
        }
        if (!isEchoClone) return;

        if (echoPath.isEmpty() || echoPathIndex >= echoPath.size()) {
            discard();
            return;
        }
        if (echoStepCooldown-- > 0) return;
        echoStepCooldown = 2;

        Vec3 next = echoPath.get(echoPathIndex++);
        teleportTo(next.x, next.y, next.z);
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.9, getZ(), 6, 0.18, 0.35, 0.18, 0.02);
        }

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
        player.displayClientMessage(Component.literal("The Doppelganger has learned that attack. Change tactics.").withStyle(ChatFormatting.RED), true);
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
        tag.putInt(NBT_CLONE_LIFETIME, cloneLifetime);
        tag.putInt("MinionSummonCooldown", minionSummonCooldown);
        tag.putInt("LifeDrainCooldown", lifeDrainCooldown);
        tag.putInt("MirrorTheftCooldown", mirrorTheftCooldown);
        tag.putInt("ShadowstepCooldown", shadowstepCooldown);
        tag.putInt("RescueTeleportCooldown", rescueTeleportCooldown);
        tag.putInt("StrandedBelowTicks", strandedBelowTicks);
        tag.putInt("MirrorCloneCooldown", mirrorCloneCooldown);
        tag.putInt("EchoCooldown", echoCooldown);
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
        cloneLifetime = tag.getInt(NBT_CLONE_LIFETIME);
        if (tag.contains("MinionSummonCooldown")) minionSummonCooldown = tag.getInt("MinionSummonCooldown");
        if (tag.contains("LifeDrainCooldown")) lifeDrainCooldown = tag.getInt("LifeDrainCooldown");
        if (tag.contains("MirrorTheftCooldown")) mirrorTheftCooldown = tag.getInt("MirrorTheftCooldown");
        if (tag.contains("ShadowstepCooldown")) shadowstepCooldown = tag.getInt("ShadowstepCooldown");
        if (tag.contains("RescueTeleportCooldown")) rescueTeleportCooldown = tag.getInt("RescueTeleportCooldown");
        if (tag.contains("StrandedBelowTicks")) strandedBelowTicks = tag.getInt("StrandedBelowTicks");
        if (tag.contains("MirrorCloneCooldown")) mirrorCloneCooldown = tag.getInt("MirrorCloneCooldown");
        if (tag.contains("EchoCooldown")) echoCooldown = tag.getInt("EchoCooldown");

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

        if (age > 45 && this.animationToPlay != null) {
            controller.forceAnimationReset();
            controller.setAnimation(animationToPlay);
            animationToPlay = null;
        }
        return spawnController.getAnimationState() == AnimationController.State.STOPPED ? PlayState.CONTINUE : PlayState.STOP;
    }

    private PlayState spawnPredicate(AnimationState<DarkDoppelgangerEntity> animationEvent) {
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
    private void playBossMusic() {
        if (!level().isClientSide && !musicPlaying && !this.isDeadOrDying()) {
            stopMinecraftAmbientMusic();
            this.level().playSound(
                    null,
                    this.getX(), this.getY(), this.getZ(),
                    SoundRegistry.BOSS_FIGHT_MUSIC.get(),
                    SoundSource.MUSIC,
                    1.0F,
                    1.0F
            );
            musicPlaying = true;
            musicTimer = MUSIC_DURATION;
        }
    }
    @Override
    public void startSeenByPlayer(@NotNull ServerPlayer player) {
        super.startSeenByPlayer(player);
        if (!this.isClone) {
            this.bossEvent.addPlayer(player);
            player.connection.send(new ClientboundStopSoundPacket(
                    ResourceLocation.fromNamespaceAndPath("minecraft", "music.game"),
                    SoundSource.MUSIC
            ));
        }
    }



    private void stopBossMusic() {
        if (!level().isClientSide && level().getServer() != null) {
            level().getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(96.0)).forEach(player ->
                    player.connection.send(new ClientboundStopSoundPacket(SoundRegistry.BOSS_FIGHT_MUSIC.get().getLocation(), SoundSource.MUSIC))
            );
        }
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