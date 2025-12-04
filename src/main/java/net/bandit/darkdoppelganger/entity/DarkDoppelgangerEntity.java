package net.bandit.darkdoppelganger.entity;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.entity.mobs.IAnimatedAttacker;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.goals.AttackAnimationData;
import io.redspace.ironsspellbooks.entity.mobs.goals.PatrolNearLocationGoal;
import io.redspace.ironsspellbooks.entity.mobs.goals.SpellBarrageGoal;
import io.redspace.ironsspellbooks.entity.mobs.wizards.GenericAnimatedWarlockAttackGoal;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import net.bandit.darkdoppelganger.Config;
import net.bandit.darkdoppelganger.DarkDoppelgangerMod;
import net.bandit.darkdoppelganger.registry.ItemRegistry;
import net.bandit.darkdoppelganger.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

import java.util.*;


public class DarkDoppelgangerEntity extends AbstractSpellCastingMob implements Enemy, IAnimatedAttacker {

    @Nullable
    private Player summonerPlayer;

    private final ServerBossEvent bossEvent;
    private boolean secondPhaseTriggered = false;
    private boolean thirdPhaseTriggered = false;
    public boolean isClone = false;
    private boolean musicPlaying = false;
    private int minionSummonCooldown = 700;
    private int lifeDrainCooldown = 150;
    private int roarSoundCooldown = 800;
    private int laughSoundCooldown = 800;
    private final List<UUID> activeMinionUUIDs = new java.util.ArrayList<>();
    private final int MAX_MINIONS = 3;
    private int laughCooldown = 800;
    private int age;
    private int musicTimer = 0;
    private static final int MUSIC_DURATION = 6160;
    private boolean hasFallenIntoVoid = false;
    private int teleportCooldown = 0;


    public DarkDoppelgangerEntity(EntityType<? extends AbstractSpellCastingMob> type, Level world) {
        super(type, world);
        this.setCustomName(Component.literal("Dark Doppelganger"));
        this.bossEvent = new ServerBossEvent(Component.literal("Dark Doppelganger"), ServerBossEvent.BossBarColor.PURPLE, ServerBossEvent.BossBarOverlay.PROGRESS);
        this.lookControl = createLookControl();
        this.moveControl = createMoveControl();
    }

    protected LookControl createLookControl() {
        return new LookControl(this) {
            //This allows us to more rapidly turn towards our target. Helps to make sure his targets are aligned with his swing animations
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

    public void setSummonerPlayer(Player summoner) {
        this.summonerPlayer = summoner;

        if (summoner != null) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                this.setItemSlot(slot, summoner.getItemBySlot(slot));
            }

            copyAttribute(AttributeRegistry.HOLY_SPELL_POWER.get());
            copyAttribute(AttributeRegistry.BLOOD_SPELL_POWER.get());
            copyAttribute(AttributeRegistry.NATURE_SPELL_POWER.get());
            copyAttribute(AttributeRegistry.ELDRITCH_SPELL_POWER.get());
            copyAttribute(AttributeRegistry.FIRE_SPELL_POWER.get());
            copyAttribute(AttributeRegistry.ICE_SPELL_POWER.get());
            copyAttribute(AttributeRegistry.LIGHTNING_SPELL_POWER.get());
            copyAttribute(AttributeRegistry.EVOCATION_SPELL_POWER.get());
            copyAttribute(AttributeRegistry.ENDER_SPELL_POWER.get());
            copyAttribute(AttributeRegistry.SPELL_POWER.get());

            boostSpellPowerFromConfig();

            if (Config.DOPPLEGANGER_HARD_MODE.get()) {
                this.getAttribute(AttributeRegistry.HOLY_MAGIC_RESIST.get()).setBaseValue(1.3f);
                this.getAttribute(AttributeRegistry.FIRE_MAGIC_RESIST.get()).setBaseValue(1.5f);
                this.getAttribute(AttributeRegistry.BLOOD_MAGIC_RESIST.get()).setBaseValue(1.5f);
                this.getAttribute(AttributeRegistry.NATURE_MAGIC_RESIST.get()).setBaseValue(1.4f);
                this.getAttribute(AttributeRegistry.ELDRITCH_MAGIC_RESIST.get()).setBaseValue(1.6f);
                this.getAttribute(AttributeRegistry.ICE_MAGIC_RESIST.get()).setBaseValue(1.4f);
                this.getAttribute(AttributeRegistry.LIGHTNING_MAGIC_RESIST.get()).setBaseValue(1.4f);
                this.getAttribute(AttributeRegistry.EVOCATION_MAGIC_RESIST.get()).setBaseValue(1.3f);
                this.getAttribute(AttributeRegistry.ENDER_MAGIC_RESIST.get()).setBaseValue(1.4f);
                this.getAttribute(AttributeRegistry.SPELL_RESIST.get()).setBaseValue(1.5f);
            }
        }
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

    protected void setSecondPhaseGoals() {
        this.goalSelector.getRunningGoals().forEach(WrappedGoal::stop);
        this.goalSelector.removeAllGoals((x) -> true);
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new SpellBarrageGoal(this, SpellRegistry.FIREBALL_SPELL.get(), 3, 5, 100, 250, 1));
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
                        List.of(SpellRegistry.MAGIC_ARROW_SPELL.get(), SpellRegistry.POISON_ARROW_SPELL.get(), SpellRegistry.MAGMA_BOMB_SPELL.get()),
                        List.of(SpellRegistry.HEAT_SURGE_SPELL.get(), SpellRegistry.FLAMING_STRIKE_SPELL.get()),
                        List.of(SpellRegistry.FROST_STEP_SPELL.get()),
                        List.of(SpellRegistry.ROOT_SPELL.get(), SpellRegistry.THUNDERSTORM_SPELL.get())
                )
        );
        this.goalSelector.addGoal(4, new PatrolNearLocationGoal(this, 30, .75f));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
    }

    protected void setThirdPhaseGoals() {
        this.goalSelector.getRunningGoals().forEach(WrappedGoal::stop);
        this.goalSelector.removeAllGoals((x) -> true);
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new SpellBarrageGoal(this, SpellRegistry.RAY_OF_FROST_SPELL.get(), 3, 5, 100, 250, 1));
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
                        List.of(SpellRegistry.LIGHTNING_LANCE_SPELL.get(), SpellRegistry.STOMP_SPELL.get()),
                        List.of(SpellRegistry.SHOCKWAVE_SPELL.get(), SpellRegistry.ASCENSION_SPELL.get()),
                        List.of(SpellRegistry.BLOOD_STEP_SPELL.get()),
                        List.of(SpellRegistry.EVASION_SPELL.get(), SpellRegistry.ECHOING_STRIKES_SPELL.get())
                )
        );
        this.goalSelector.addGoal(4, new PatrolNearLocationGoal(this, 30, .75f));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
    }

    protected void setFinalPhaseGoals() {
        this.goalSelector.getRunningGoals().forEach(WrappedGoal::stop);
        this.goalSelector.removeAllGoals((x) -> true);
        this.goalSelector.addGoal(1, new FloatGoal(this));

        List<AbstractSpell> allSpells = new ArrayList<>(getConfiguredSpells(Config.DOPPELGANGER_FINAL_PHASE_SPELLS.get()));
        Collections.shuffle(allSpells, new Random(this.getRandom().nextLong()));
        // fallback to Eldritch Blast
        AbstractSpell barrageSpell = allSpells.isEmpty()
                ? SpellRegistry.ELDRITCH_BLAST_SPELL.get()
                : allSpells.get(0);

        this.goalSelector.addGoal(2, new SpellBarrageGoal(this, barrageSpell, 3, 4, 160, 240, 1));

        // Utility
        List<AbstractSpell> group1 = getSpellGroup(allSpells, 0, 3);
        List<AbstractSpell> group2 = getSpellGroup(allSpells, 3, 3);
        List<AbstractSpell> group3 = getSpellGroup(allSpells, 6, 2);
        List<AbstractSpell> group4 = getSpellGroup(allSpells, 8, 4);

        this.goalSelector.addGoal(3, new GenericAnimatedWarlockAttackGoal<>(this, 1.4f, 30, 50, 3f)
                .setMoveset(List.of(
                        new AttackAnimationData(9, "simple_sword_upward_swipe", 5),
                        new AttackAnimationData(8, "simple_sword_lunge_stab", 6),
                        new AttackAnimationData(10, "simple_sword_stab_alternate", 8),
                        new AttackAnimationData(10, "simple_sword_horizontal_cross_swipe", 8)
                ))
                .setComboChance(0.4f)
                .setMeleeAttackInverval(10, 20)
                .setMeleeMovespeedModifier(1.7f)
                .setSpells(group1, group2, group3, group4)
        );

        this.goalSelector.addGoal(5, new PatrolNearLocationGoal(this, 30, 0.75f));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
    }

    private List<AbstractSpell> getSpellGroup(List<AbstractSpell> list, int start, int count) {
        if (start >= list.size()) return List.of();
        return list.subList(start, Math.min(start + count, list.size()));
    }


    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
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
            if (!this.isClone) {
                adjustAttributesFromConfig();
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


    private void copyAttribute(net.minecraft.world.entity.ai.attributes.Attribute attribute) {
        AttributeInstance sourceAttribute = this.summonerPlayer.getAttribute(attribute);
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
    private void boostSpellPowerFromConfig() {
        double multiplier = Config.DOPPELGANGER_SPELL_POWER_MULTIPLIER.get();
        if (multiplier <= 0.0 || multiplier == 1.0) {
            return; // no change
        }

        scaleSpellPower(AttributeRegistry.HOLY_SPELL_POWER.get(), multiplier);
        scaleSpellPower(AttributeRegistry.BLOOD_SPELL_POWER.get(), multiplier);
        scaleSpellPower(AttributeRegistry.NATURE_SPELL_POWER.get(), multiplier);
        scaleSpellPower(AttributeRegistry.ELDRITCH_SPELL_POWER.get(), multiplier);
        scaleSpellPower(AttributeRegistry.FIRE_SPELL_POWER.get(), multiplier);
        scaleSpellPower(AttributeRegistry.ICE_SPELL_POWER.get(), multiplier);
        scaleSpellPower(AttributeRegistry.LIGHTNING_SPELL_POWER.get(), multiplier);
        scaleSpellPower(AttributeRegistry.EVOCATION_SPELL_POWER.get(), multiplier);
        scaleSpellPower(AttributeRegistry.ENDER_SPELL_POWER.get(), multiplier);
        scaleSpellPower(AttributeRegistry.SPELL_POWER.get(), multiplier);
    }

    private void scaleSpellPower(net.minecraft.world.entity.ai.attributes.Attribute attribute, double multiplier) {
        AttributeInstance inst = this.getAttribute(attribute);
        if (inst != null) {
            inst.setBaseValue(inst.getBaseValue() * multiplier);
        }
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

    private void applyAttributesFromConfig() {
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

    private void stopAllMusic() {
        if (!level().isClientSide && level().getServer() != null) {
            Objects.requireNonNull(level().getServer()).getPlayerList().getPlayers().forEach(player -> {
                player.connection.send(new ClientboundStopSoundPacket(null, SoundSource.MUSIC));
            });
        }
    }

    private void stopMinecraftAmbientMusic() {
        if (!level().isClientSide && level().getServer() != null) {
            for (ServerPlayer player : Objects.requireNonNull(level().getServer()).getPlayerList().getPlayers()) {
                player.connection.send(new ClientboundStopSoundPacket(new ResourceLocation("minecraft:music.game"), SoundSource.MUSIC));
                player.connection.send(new ClientboundStopSoundPacket(new ResourceLocation("minecraft:music.creative"), SoundSource.MUSIC));
                player.connection.send(new ClientboundStopSoundPacket(new ResourceLocation("minecraft:music.menu"), SoundSource.MUSIC));
                player.connection.send(new ClientboundStopSoundPacket(new ResourceLocation("minecraft:music.overworld.day"), SoundSource.MUSIC));
                player.connection.send(new ClientboundStopSoundPacket(new ResourceLocation("minecraft:music.overworld.night"), SoundSource.MUSIC));
                player.connection.send(new ClientboundStopSoundPacket(new ResourceLocation("minecraft:music.overworld.hills"), SoundSource.MUSIC));
                player.connection.send(new ClientboundStopSoundPacket(new ResourceLocation("minecraft:music.overworld.water"), SoundSource.MUSIC));
            }
        }
    }

    @Override
    public void startSeenByPlayer(@NotNull ServerPlayer player) {
        super.startSeenByPlayer(player);
        if (!this.isClone) {
            this.bossEvent.addPlayer(player);
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
        if (!level().isClientSide && isClone && !hasFallenIntoVoid && level().dimension() == Level.END && this.getY() < -100) {
            Player target = null;

            // Clones look for summoner; minions just look for nearest player
            if (this.getTags().contains("dark_doppelganger_clone")) {
                target = getSummonerPlayer(); // Might be null
            }
            if (target == null) {
                target = level().getNearestPlayer(this, 64);
            }

            if (target != null && !target.isDeadOrDying()) {
                this.teleportTo(target.getX(), target.getY() + 1.5, target.getZ());
                this.setYRot(target.getYRot());
                hasFallenIntoVoid = true;
                teleportCooldown = 100;
            }
        }
        if (teleportCooldown > 0) {
            teleportCooldown--;
            if (teleportCooldown == 0) {
                hasFallenIntoVoid = false;
            }
        }

        if (isClone || this.isDeadOrDying()) return;

        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());

        if (musicPlaying) {
            stopMinecraftAmbientMusic();
            if (musicTimer > 0) {
                musicTimer--;
            } else {
                playBossMusic();
            }
        }
        if (!musicPlaying) {
            stopMinecraftAmbientMusic();
            playBossMusic();
        }

        if (!isClone && laughCooldown > 0) {
            laughCooldown--;
        }

        if (this.getHealth() < this.getMaxHealth() * 0.4 && minionSummonCooldown <= 0) {
            summonMinions();
            minionSummonCooldown = 1000;
        }
        if (!level().isClientSide && !isClone && !hasFallenIntoVoid && level().dimension() == Level.END && this.getY() < -100) {
            Player summoner = this.getSummonerPlayer();
            if (summoner != null && !summoner.isDeadOrDying()) {
                double targetX = summoner.getX();
                double targetY = summoner.getY() + 1.5;
                double targetZ = summoner.getZ();

                // Exit portal at original position
                PortalJoinEntity exitPortal = new PortalJoinEntity(EntityRegistry.PORTAL_JOIN_ENTITY.get(), this.level());
                exitPortal.setPos(this.position());
                this.level().addFreshEntity(exitPortal);

                this.teleportTo(targetX, targetY, targetZ);
                this.setYRot(summoner.getYRot());

                this.level().playSound(null, summoner.blockPosition(), SoundEvents.PORTAL_TRAVEL, SoundSource.HOSTILE, 1.0F, 1.0F);

                // Entrance portal at new location
                PortalJoinEntity entrancePortal = new PortalJoinEntity(EntityRegistry.PORTAL_JOIN_ENTITY.get(), this.level());
                entrancePortal.setPos(targetX, targetY - 1.5, targetZ);
                this.level().addFreshEntity(entrancePortal);

                summoner.sendSystemMessage(Component.literal("The Dark Doppelganger has returned from the void...").withStyle(ChatFormatting.DARK_PURPLE));

                hasFallenIntoVoid = true;
                teleportCooldown = 100;
            }
        }

        if (Config.DOPPLEGANGER_HARD_MODE.get()) {
            this.addEffect(new MobEffectInstance(MobEffectRegistry.OAKSKIN.get(), 10, 8, false, false, true));
            this.addEffect(new MobEffectInstance(MobEffectRegistry.CHARGED.get(), 10, 2, false, false, true));
            this.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 10, 0, false, false));
        }

        if (Config.DOPPLEGANGER_HARD_MODE.get()) {
            if (this.tickCount % 10 == 0) {
                this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(20, 10, 20)).forEach(target -> {
                    if (target != this) {
                        if (target.hasEffect(MobEffects.DIG_SPEED)) {
                            target.removeEffect(MobEffects.DIG_SPEED);
                        }
                        if (target.hasEffect(MobEffectRegistry.ABYSSAL_SHROUD.get())) {
                            target.removeEffect(MobEffectRegistry.ABYSSAL_SHROUD.get());
                        }
                        if (target.hasEffect(MobEffectRegistry.EVASION.get())) {
                            target.removeEffect(MobEffectRegistry.EVASION.get());
                        }
                        if (target.hasEffect(MobEffectRegistry.HASTENED.get())) {
                            target.removeEffect(MobEffectRegistry.HASTENED.get());
                        }
                        if (target.hasEffect(MobEffectRegistry.ECHOING_STRIKES.get())) {
                            target.removeEffect(MobEffectRegistry.ECHOING_STRIKES.get());
                        }
                    }
                });
            }
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
        if (laughSoundCooldown > 0) laughSoundCooldown--;

        age++;
    }

    private void playBossMusic() {
        if (!level().isClientSide && !musicPlaying && !this.isDeadOrDying()) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    ModSounds.BOSS_FIGHT_MUSIC.get(), SoundSource.MUSIC, 1.0F, 1.0F);
            musicPlaying = true;
            musicTimer = MUSIC_DURATION; // Set timer to song duration
        }
    }


    private void stopBossMusic() {
        if (!level().isClientSide && level().getServer() != null) {
            Objects.requireNonNull(level().getServer()).getPlayerList().getPlayers().forEach(player -> {
                player.connection.send(new ClientboundStopSoundPacket(ModSounds.BOSS_FIGHT_MUSIC.get().getLocation(), SoundSource.MUSIC));
            });
        }
    }

    @Nullable
    public Player getSummonerPlayer() {
        return summonerPlayer;
    }


    @Override
    public boolean addEffect(MobEffectInstance p_147208_, @Nullable Entity p_147209_) {
        if (!p_147208_.getEffect().isBeneficial()) {
            return false;
        }
        return super.addEffect(p_147208_, p_147209_);
    }

    private void triggerSecondPhase() {
        secondPhaseTriggered = true;
        setHealth(getMaxHealth());

        for (ServerPlayer player : level().getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(50))) {
            player.sendSystemMessage(Component.literal("The Dark Doppelganger has entered its Second Phase!").withStyle(ChatFormatting.DARK_PURPLE));
            level().playSound(null, getX(), getY(), getZ(), ModSounds.BOSS_ROAR.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
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
        bossEvent.setName(Component.literal("Dark Doppelganger - Final Phase"));

        for (ServerPlayer player : level().getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(50))) {
            level().playSound(null, getX(), getY(), getZ(), ModSounds.BOSS_ROAR.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
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
        if (source == this.level().damageSources().fellOutOfWorld()) {
            return false;
        }
        if (this.isDeadOrDying()) {
            return false;
        }

        if (isClone || this.getTags().contains("dark_doppelganger_clone")) {
            return super.hurt(source, amount);
        }

        double cap = Config.DOPPELGANGER_DAMAGE_CAP.get();
        if (cap > 0.0 && amount > (float) cap) {
            amount = (float) cap;
        }

        float newHealth = this.getHealth() - amount;

        if (!secondPhaseTriggered && newHealth <= this.getMaxHealth() * 0.1f) {
            triggerSecondPhase();

            if (Config.DOPPLEGANGER_HARD_MODE.get()) {
                setThirdPhaseGoals();
            } else {
                setSecondPhaseGoals();
            }
            return false;
        }

        if (!thirdPhaseTriggered && newHealth <= this.getMaxHealth() * 0.1f) {
            triggerThirdPhase();
            if (Config.DOPPLEGANGER_HARD_MODE.get()) {
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

    private void summonMinions() {
        if (isClone || minionSummonCooldown > 0 || activeMinionUUIDs.size() >= MAX_MINIONS) return;

        for (int i = 0; i < 2; i++) {
            if (activeMinionUUIDs.size() >= MAX_MINIONS) break;

            DarkDoppelgangerMinionEntity minion = EntityRegistry.DARK_DOPPELGANGER_MINION.get().create(level());
            if (minion != null) {
                minion.setPos(getX() + random.nextInt(5) - 2, getY(), getZ() + random.nextInt(5) - 2);
                minion.setHealth(minion.getMaxHealth() * 0.3F);
                minion.setSummonerUUID(this.getUUID());
                minion.addTag("dark_doppelganger_clone");

                Team team = getTeam();
                if (team instanceof PlayerTeam playerTeam) {
                    level().getScoreboard().addPlayerToTeam(minion.getScoreboardName(), playerTeam);
                }

                minion.setCustomName(Component.literal("Doppelganger Minion").withStyle(ChatFormatting.DARK_GRAY));

                level().addFreshEntity(minion);
                activeMinionUUIDs.add(minion.getUUID());
            }
        }

        minionSummonCooldown = 500;
    }



    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    public void removeMinion(UUID uuid) {
        activeMinionUUIDs.remove(uuid);
    }


    private void lifeDrainAttack() {
        level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(8)).forEach(player -> {
            player.hurt(level().damageSources().magic(), 4.0F);
            heal(4.0F);
        });

        if (laughCooldown <= 0) {
            level().playSound(null, getX(), getY(), getZ(), ModSounds.BOSS_LAUGH.get(), SoundSource.HOSTILE, 0.0F, 1.0F);
            laughCooldown = 400;
        }
    }
    @Override
    public void die(@NotNull DamageSource cause) {
        if (isClone) {
            for (Mob mob : level().getEntitiesOfClass(Mob.class, getBoundingBox().inflate(64))) {
                if (mob instanceof DarkDoppelgangerEntity doppel && !doppel.isClone) {
                    doppel.removeMinion(this.getUUID());
                    break;
                }
            }
            this.level().addParticle(ParticleTypes.POOF, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
            super.die(cause);
            return;
        }

        // Main boss death logic
        if (!thirdPhaseTriggered) {
            this.setHealth(1.0F);
            return;
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            this.setItemSlot(slot, ItemStack.EMPTY);
        }
        if (musicPlaying) {
            stopBossMusic();
            musicPlaying = false;
            musicTimer = 0;
        }
        if (!this.level().isClientSide) {
            if (cause.getEntity() instanceof ServerPlayer serverPlayer) {
                Advancement advancement = Objects.requireNonNull(serverPlayer.getServer()).getAdvancements()
                        .getAdvancement(new ResourceLocation("darkdoppelganger", "kill_dark_doppelganger"));

                if (advancement != null) {
                    serverPlayer.getAdvancements().award(advancement, "kill");
                    serverPlayer.sendSystemMessage(Component.literal("You have slain the Dark Doppelganger!"));
                }
            }

            List<Item> lootPool = List.of(
                    ItemRegistry.DOPPELGANGER_RING.get(),
                    ItemRegistry.ELDER_NECKLACE.get(),
                    ItemRegistry.SUMMONS_NECKLACE.get()
            );

            Item selectedDrop = Util.getRandom(lootPool, this.getRandom());
            this.spawnAtLocation(selectedDrop);
            this.spawnAtLocation(Items.NETHER_STAR);
            this.spawnAtLocation(Items.ECHO_SHARD, 3);
            this.spawnAtLocation(Items.DIAMOND_BLOCK, 3);
            this.level().addFreshEntity(new ExperienceOrb(this.level(), this.getX(), this.getY(), this.getZ(), 2500));
        }

        this.bossEvent.removeAllPlayers();
        super.die(cause);
    }
    @Override
    protected void dropAllDeathLoot(DamageSource source) {
        // Prevent any default drops
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        // Prevent any custom loot drops
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
            DarkDoppelgangerMod.LOGGER.error("Entity {} Failed to play animation: {}", this, animationId);
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
}