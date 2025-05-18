package net.bandit.darkdoppelganger.entity;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
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
import net.bandit.darkdoppelganger.registry.ItemRegistry;
import net.bandit.darkdoppelganger.registry.SoundRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
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
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.animation.AnimationState;

import java.util.List;
import java.util.Objects;

public class DarkDoppelgangerEntity extends AbstractSpellCastingMob implements Enemy, IAnimatedAttacker {

    @Nullable
    private Player summonerPlayer;

    private final ServerBossEvent bossEvent;
    private boolean secondPhaseTriggered = false;
    private boolean thirdPhaseTriggered = false;
    public boolean isClone = false;
    private boolean musicPlaying = false;
    private int minionSummonCooldown = 300;
    private int lifeDrainCooldown = 150;
    private int roarSoundCooldown = 800;
    private int laughSoundCooldown = 800;
    private static final int MAX_MINIONS = 5;
    private static int currentMinionCount = 0;
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
    this.summonerPlayer = summoner;
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
                        List.of(SpellRegistry.ELDRITCH_BLAST_SPELL.get(), SpellRegistry.SONIC_BOOM_SPELL.get(), SpellRegistry.ABYSSAL_SHROUD_SPELL.get(), SpellRegistry.RAY_OF_FROST_SPELL.get(), SpellRegistry.SCULK_TENTACLES_SPELL.get()),
                        List.of(SpellRegistry.ASCENSION_SPELL.get(), SpellRegistry.ABYSSAL_SHROUD_SPELL.get()),
                        List.of(SpellRegistry.BLOOD_STEP_SPELL.get()),
                        List.of(SpellRegistry.ABYSSAL_SHROUD_SPELL.get(), SpellRegistry.ECHOING_STRIKES_SPELL.get(), SpellRegistry.ROOT_SPELL.get(), SpellRegistry.BLIGHT_SPELL.get())
                )
        );
        this.goalSelector.addGoal(5, new PatrolNearLocationGoal(this, 30, .75f));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
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

    private void copyAttribute(Holder<Attribute> attribute) {
        if(this.summonerPlayer == null){return;}
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
            for (ServerPlayer player : Objects.requireNonNull(level().getServer()).getPlayerList().getPlayers()) {
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
            playBossMusic();
        }
        if (!isClone && laughCooldown > 0) {
            laughCooldown--;
        }
        if (this.getHealth() < this.getMaxHealth() * 0.4 && minionSummonCooldown <= 0) {
            summonMinions();
            minionSummonCooldown = 1000;
        }
        if (!level().isClientSide && !hasFallenIntoVoid && level().dimension() == Level.END && this.getY() < -100) {
            Player summoner = this.getSummonerPlayer();
            if (summoner != null && !summoner.isDeadOrDying()) {
                PortalJoinEntity exitPortal = new PortalJoinEntity(EntityRegistry.PORTAL_JOIN_ENTITY.get(), this.level());
                exitPortal.setPos(this.position());
                this.level().addFreshEntity(exitPortal);
                double targetX = summoner.getX();
                double targetY = summoner.getY() + 1.5;
                double targetZ = summoner.getZ();

                this.teleportTo(targetX, targetY, targetZ);
                this.setYRot(summoner.getYRot());
                this.level().playSound(null, summoner.blockPosition(), SoundEvents.PORTAL_TRAVEL, SoundSource.HOSTILE, 1.0F, 1.0F);

                PortalJoinEntity entrancePortal = new PortalJoinEntity(EntityRegistry.PORTAL_JOIN_ENTITY.get(), this.level());
                entrancePortal.setPos(targetX, targetY - 1.5, targetZ);
                this.level().addFreshEntity(entrancePortal);

                summoner.sendSystemMessage(Component.literal("The Dark Doppelganger has returned from the void...").withStyle(ChatFormatting.DARK_PURPLE));

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

        if (this.getHealth() < this.getMaxHealth() * 0.4 && minionSummonCooldown <= 0) {
            summonMinions();
            minionSummonCooldown = 1000;
        }

        if (Config.DOPPELGANGER_HARD_MODE.get()) {
            this.addEffect(new MobEffectInstance(MobEffectRegistry.OAKSKIN.getDelegate(), 10, 8, false, false, true));
            this.addEffect(new MobEffectInstance( MobEffectRegistry.CHARGED.getDelegate(), 10, 2, false, false, true));
            this.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 10, 0, false, false));
        }

        if (Config.DOPPELGANGER_HARD_MODE.get()) {
            if (this.tickCount % 10 == 0) {
                this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(20, 10, 20)).forEach(target -> {
                    if (target != this) {
                        if (target.hasEffect(MobEffects.DIG_SPEED)) {
                            target.removeEffect(MobEffects.DIG_SPEED);
                        }
                        if (target.hasEffect(MobEffectRegistry.ABYSSAL_SHROUD.getDelegate())) {
                            target.removeEffect(MobEffectRegistry.ABYSSAL_SHROUD.getDelegate());
                        }
                        if (target.hasEffect( MobEffectRegistry.EVASION.getDelegate())) {
                            target.removeEffect(MobEffectRegistry.EVASION.getDelegate());
                        }
                        if (target.hasEffect(MobEffectRegistry.HASTENED.getDelegate())) {
                            target.removeEffect( MobEffectRegistry.HASTENED.getDelegate());
                        }
                        if (target.hasEffect(MobEffectRegistry.ECHOING_STRIKES.getDelegate())) {
                            target.removeEffect( MobEffectRegistry.ECHOING_STRIKES.getDelegate());
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
    @Nullable
    public Player getSummonerPlayer() {
        return summonerPlayer;
    }
    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    private void triggerSecondPhase() {
        secondPhaseTriggered = true;
        setHealth(getMaxHealth());

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
        bossEvent.setName(Component.literal("Dark Doppelganger - Final Phase"));

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
        if (this.isDeadOrDying() || source == this.level().damageSources().fellOutOfWorld()) {
            return false;
        }
        if (isClone) {
            return super.hurt(source, amount);
        }
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

    private void summonMinions() {
        if (isClone || minionSummonCooldown > 0 || currentMinionCount >= MAX_MINIONS) return;

        for (int i = 0; i < 2; i++) {
            if (currentMinionCount >= MAX_MINIONS) break;

            DarkDoppelgangerMinionEntity minion = (DarkDoppelgangerMinionEntity) EntityRegistry.DARK_DOPPELGANGER_MINION.get().create(level());
            if (minion != null) {
                minion.setPos(getX() + random.nextInt(5) - 2, getY(), getZ() + random.nextInt(5) - 2);
                minion.setSummonerUUID(this.getUUID()); // Tie to boss
                minion.setHealth(minion.getMaxHealth());

                Team team = getTeam();
                if (team instanceof PlayerTeam playerTeam) {
                    level().getScoreboard().addPlayerToTeam(minion.getScoreboardName(), playerTeam);
                }

                minion.setCustomName(Component.literal("Doppelganger Minion").withStyle(ChatFormatting.DARK_GRAY));
                level().addFreshEntity(minion);
                currentMinionCount++;
            }
        }
        minionSummonCooldown = 500;
    }

    private void lifeDrainAttack() {
        level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(8)).forEach(player -> {
            player.hurt(level().damageSources().magic(), 4.0F);
            heal(4.0F);
        });

        if (laughCooldown <= 0) {
            level().playSound(null, getX(), getY(), getZ(), SoundRegistry.BOSS_LAUGH.get(), SoundSource.HOSTILE, 0.0F, 1.0F);
            laughCooldown = 400;
        }
    }

    @Override
    public void die(@NotNull DamageSource cause) {
        // Handle death for clones
        if (isClone) {
            synchronized (DarkDoppelgangerEntity.class) {
                currentMinionCount = Math.max(0, currentMinionCount - 1);
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
            int choice = this.random.nextInt(3);
            switch (choice) {
                case 0 -> this.spawnAtLocation(ItemRegistry.DOPPELGANGER_RING.get());
                case 1 -> this.spawnAtLocation(ItemRegistry.ELDER_NECKLACE.get());
                case 2 -> this.spawnAtLocation(ItemRegistry.SUMMONS_NECKLACE.get());
            }
            this.spawnAtLocation(Items.NETHER_STAR);
            this.spawnAtLocation(Items.ECHO_SHARD, 3);
            this.spawnAtLocation(Items.DIAMOND_BLOCK, 3);

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
        super.die(cause);
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
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                   SoundRegistry.BOSS_FIGHT_MUSIC.get(), SoundSource.MUSIC, 1.0F, 1.0F);
            musicPlaying = true;
            musicTimer = MUSIC_DURATION; // Set timer to song duration
        }
    }


    private void stopBossMusic() {
        if (!level().isClientSide && level().getServer() != null) {
            Objects.requireNonNull(level().getServer()).getPlayerList().getPlayers().forEach(player -> {
                player.connection.send(new ClientboundStopSoundPacket(SoundRegistry.BOSS_FIGHT_MUSIC.get().getLocation(), SoundSource.MUSIC));
            });
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