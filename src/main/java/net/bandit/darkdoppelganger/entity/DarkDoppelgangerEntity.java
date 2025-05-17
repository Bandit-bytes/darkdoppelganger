package net.bandit.darkdoppelganger.entity;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.api.network.IClientEventEntity;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.util.FogManager;
import io.redspace.ironsspellbooks.api.util.MusicManager;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.entity.mobs.IAnimatedAttacker;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.goals.MomentHurtByTargetGoal;
import io.redspace.ironsspellbooks.entity.mobs.goals.PatrolNearLocationGoal;
import io.redspace.ironsspellbooks.entity.mobs.goals.SpellBarrageGoal;
import io.redspace.ironsspellbooks.entity.mobs.goals.melee.AttackAnimationData;
import io.redspace.ironsspellbooks.entity.mobs.goals.melee.AttackKeyframe;
import io.redspace.ironsspellbooks.entity.mobs.wizards.GenericAnimatedWarlockAttackGoal;
import io.redspace.ironsspellbooks.entity.mobs.wizards.fire_boss.FireBossMoveControl;
import io.redspace.ironsspellbooks.entity.mobs.wizards.fire_boss.InvokeDaggerKeyframe;
import io.redspace.ironsspellbooks.entity.mobs.wizards.fire_boss.NotIdioticNavigation;
import io.redspace.ironsspellbooks.network.EntityEventPacket;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import net.bandit.darkdoppelganger.Config;
import net.bandit.darkdoppelganger.DarkDoppelgangerMod;
import net.bandit.darkdoppelganger.entity.ai.*;
import net.bandit.darkdoppelganger.registry.EntityRegistry;
import net.bandit.darkdoppelganger.registry.ItemRegistry;
import net.bandit.darkdoppelganger.util.ModTags;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
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
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.AnimationState;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class DarkDoppelgangerEntity extends AbstractSpellCastingMob implements Enemy, IAnimatedAttacker, IClientEventEntity{
    public static final byte CLIENT_STOP_TRACKING = 0;
    public static final byte CLIENT_START_TRACKING = 1;
    public static final byte PROC_SPECTRAL_DAGGER = 6;

    public static final int UNLOADED_DESPAWN_LIMIT_SECONDS = 300;

    private boolean canAnimateOver;
    private int destroyBlockDelay;

    public float isAnimatingDampener;

    @Nullable
    private Player summonerPlayer;
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
        this.lookControl = createLookControl();
        this.moveControl = createMoveControl();

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
            this.getAttribute(AttributeRegistry.HOLY_SPELL_POWER).setBaseValue(1.3);
            this.getAttribute(AttributeRegistry.FIRE_MAGIC_RESIST).setBaseValue(1.5f);
            this.getAttribute(AttributeRegistry.BLOOD_MAGIC_RESIST).setBaseValue(1.5f);
            this.getAttribute(AttributeRegistry.ELDRITCH_MAGIC_RESIST).setBaseValue(1.4f);
            this.getAttribute(AttributeRegistry.ICE_MAGIC_RESIST).setBaseValue(1.6f);
            this.getAttribute(AttributeRegistry.LIGHTNING_MAGIC_RESIST).setBaseValue(1.4f);
            this.getAttribute(AttributeRegistry.EVOCATION_MAGIC_RESIST).setBaseValue(1.3f);
            this.getAttribute(AttributeRegistry.ENDER_MAGIC_RESIST).setBaseValue(1.4f);
            this.getAttribute(AttributeRegistry.SPELL_RESIST).setBaseValue(1.5f);
        }
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        this.setPersistenceRequired();
        if (this.isClone) {
            this.addTag(ModTags.CLONES.toString());
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
        int extraPlayers = Math.max(0, playerScale - 1);
        double extraHealthPercent = extraPlayers * 0.40 + extraPlayers * extraPlayers * 0.10;

        if (Config.DOPPELGANGER_HEALTH != null) {
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(Config.DOPPELGANGER_HEALTH.get() + (Config.DOPPELGANGER_HEALTH.get() * extraHealthPercent));
        }
        if (Config.DOPPELGANGER_ATTACK_DAMAGE != null) {
            this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(Config.DOPPELGANGER_ATTACK_DAMAGE.get() + (Config.DOPPELGANGER_ATTACK_DAMAGE.get() * 0.25 * extraPlayers));
        }
        if (Config.DOPPELGANGER_MOVEMENT_SPEED != null) {
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(Config.DOPPELGANGER_MOVEMENT_SPEED.get());
        }
        if (Config.DOPPELGANGER_KNOCKBACK_RESISTANCE != null) {
            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(Config.DOPPELGANGER_KNOCKBACK_RESISTANCE.get());
        }
        if (Config.DOPPELGANGER_ARMOR != null) {
            this.getAttribute(Attributes.ARMOR).setBaseValue(Config.DOPPELGANGER_ARMOR.get() + (Config.DOPPELGANGER_ARMOR.get() * 0.25 * extraPlayers));
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

    @Nullable
    public Player getSummonerPlayer() {
        return summonerPlayer;
    }

    private void triggerSecondPhase() {
        secondPhaseTriggered = true;
        setHealth(getMaxHealth());

        for (ServerPlayer player : level().getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(50))) {
            player.sendSystemMessage(Component.literal("The Dark Doppelganger has entered its Second Phase!").withStyle(ChatFormatting.DARK_PURPLE));
            level().playSound(null, getX(), getY(), getZ(), net.bandit.darkdoppelganger.registry.SoundRegistry.BOSS_ROAR.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
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
            level().playSound(null, getX(), getY(), getZ(), net.bandit.darkdoppelganger.registry.SoundRegistry.BOSS_ROAR.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
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

    private void summonIllusionClones() {
        if (minionSummonCooldown > 0 || currentMinionCount >= MAX_MINIONS) return;
        for (int i = 0; i < 3; i++) {
            if (currentMinionCount >= MAX_MINIONS) break;

            DarkDoppelgangerEntity clone = EntityRegistry.DARK_DOPPELGANGER.get().create(level());
            if (clone != null) {
                clone.setPos(getX() + random.nextInt(5) - 2, getY(), getZ() + random.nextInt(5) - 2);
                clone.setHealth(10.0F);
                clone.isClone = true;
                Team team = getTeam();
                if (team instanceof PlayerTeam playerTeam) {
                    level().getScoreboard().addPlayerToTeam(clone.getScoreboardName(), playerTeam);
                }
                clone.addTag("dark_doppelganger_clone");
                clone.setCustomName(Component.literal("Doppelganger Clone").withStyle(ChatFormatting.GRAY));
                clone.applyAttributesFromConfig();
                level().addFreshEntity(clone);
                level().addParticle(ParticleTypes.ENCHANT, clone.getX(), clone.getY(), clone.getZ(), 0, 1, 0);
                currentMinionCount++;
            }
        }
        minionSummonCooldown = 500;
    }

    private void summonMinions() {
        if (isClone || minionSummonCooldown > 0 || currentMinionCount >= MAX_MINIONS) return;

        for (int i = 0; i < 2; i++) {
            if (currentMinionCount >= MAX_MINIONS) break;

            DarkDoppelgangerEntity minion = EntityRegistry.DARK_DOPPELGANGER.get().create(level());
            if (minion != null) {
                minion.setPos(getX() + random.nextInt(5) - 2, getY(), getZ() + random.nextInt(5) - 2);
                minion.setHealth(minion.getMaxHealth() * 0.3F);
                minion.isClone = true;
                minion.addTag("dark_doppelganger_clone");
                Team team = getTeam();
                if (team instanceof PlayerTeam playerTeam) {
                    level().getScoreboard().addPlayerToTeam(minion.getScoreboardName(), playerTeam);
                }

                minion.setCustomName(Component.literal("Doppelganger Minion").withStyle(ChatFormatting.DARK_GRAY));
                level().addFreshEntity(minion);
                minion.applyAttributesFromConfig();
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
            level().playSound(null, getX(), getY(), getZ(), net.bandit.darkdoppelganger.registry.SoundRegistry.BOSS_LAUGH.get(), SoundSource.HOSTILE, 0.0F, 1.0F);
            laughCooldown = 400;
        }
    }

    private void playBossMusic() {
        if (!level().isClientSide && !musicPlaying && !this.isDeadOrDying()) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    net.bandit.darkdoppelganger.registry.SoundRegistry.BOSS_FIGHT_MUSIC.get(), SoundSource.MUSIC, 1.0F, 1.0F);
            musicPlaying = true;
            musicTimer = MUSIC_DURATION; // Set timer to song duration
        }
    }


    private void stopBossMusic() {
        if (!level().isClientSide && level().getServer() != null) {
            Objects.requireNonNull(level().getServer()).getPlayerList().getPlayers().forEach(player -> {
                player.connection.send(new ClientboundStopSoundPacket(net.bandit.darkdoppelganger.registry.SoundRegistry.BOSS_FIGHT_MUSIC.get().getLocation(), SoundSource.MUSIC));
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

    @Override
    public void handleClientEvent(byte eventId) {
        switch (eventId) {
            case CLIENT_STOP_TRACKING -> {
                FogManager.stopEvent(this.uuid);
            }
            case CLIENT_START_TRACKING -> {
                FogManager.createEvent(this, new FogManager.FogEvent(Optional.empty(), true));
            }
            case PROC_SPECTRAL_DAGGER -> procSpectralDagger();
        }
    }

    @Override
    public void die(@NotNull DamageSource cause) {
        // Handle death for clones
        if (isClone) {
            synchronized (DarkDoppelgangerEntity_old.class) {
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
            this.spawnAtLocation(ItemRegistry.DOPPELGANGER_RING.get());
            this.spawnAtLocation(Items.NETHER_STAR);
            this.spawnAtLocation(Items.ECHO_SHARD, 3);
            this.spawnAtLocation(Items.DIAMOND_BLOCK, 3);

            this.level().addFreshEntity(new ExperienceOrb(this.level(), this.getX(), this.getY(), this.getZ(), 2500));
        }

        if (this.level().getServer() != null) {
            this.level().getServer().getPlayerList().getPlayers().forEach(player -> {
                if (player.connection != null) {
                    player.connection.send(
                            new ClientboundStopSoundPacket(net.bandit.darkdoppelganger.registry.SoundRegistry.BOSS_FIGHT_MUSIC.get().getLocation(), SoundSource.MUSIC)
                    );
                }
            });
        }
        if (this.bossEvent != null) {
            this.bossEvent.removeAllPlayers();
        }
        super.die(cause);
    }

    private int playerScale;

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder pBuilder) {
        super.defineSynchedData(pBuilder);
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
        return new FireBossMoveControl(this);
    }

    public void startSeenByPlayer(ServerPlayer pPlayer) {
        super.startSeenByPlayer(pPlayer);
        if (!this.isClone) {
            this.bossEvent.addPlayer(pPlayer);
        }
        PacketDistributor.sendToPlayer(pPlayer, new EntityEventPacket<DarkDoppelgangerEntity>(this, CLIENT_START_TRACKING));
    }

    public void stopSeenByPlayer(ServerPlayer pPlayer) {
        super.stopSeenByPlayer(pPlayer);
        if (!this.isClone) {
            this.bossEvent.removePlayer(pPlayer);
        }
        PacketDistributor.sendToPlayer(pPlayer, new EntityEventPacket<DarkDoppelgangerEntity>(this, CLIENT_STOP_TRACKING));
    }

    public EnderBossAttackGoal attackGoal;

    @Override
    public FireBossMoveControl getMoveControl() {
        return (FireBossMoveControl) super.getMoveControl();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.attackGoal = (EnderBossAttackGoal) new EnderBossAttackGoal(this, 1.5f, 50, 75)
                .setMoveset(List.of(
                        AttackAnimationData.builder("scythe_dagger_double_horizontal")
                                .length(60)
                                .attacks(
                                        new EnderBossAttackKeyframe(15, new Vec3(0, 0, .25), new EnderBossAttackKeyframe.SwingData(false, true)),
                                        new InvokeDaggerKeyframe(35),
                                        new EnderBossAttackKeyframe(36, new Vec3(0, 0, .75), new EnderBossAttackKeyframe.SwingData(false, false)),
                                        new AttackKeyframe(42, new Vec3(0, 0, 0))
                                ).build(),
                        AttackAnimationData.builder("scythe_backpedal")
                                .length(40)
                                .rangeMultiplier(2f)
                                .attacks(
                                        new EnderBossAttackKeyframe(20, new Vec3(0, .3, -2), new EnderBossAttackKeyframe.SwingData(false, true))
                                ).build(),
                        AttackAnimationData.builder("scythe_sideslash_downslash_sideslash")
                                .length(62)
                                .rangeMultiplier(2f)
                                .attacks(
                                        new EnderBossAttackKeyframe(18, new Vec3(0, 0, .45), new EnderBossAttackKeyframe.SwingData(false, true)),
                                        new EnderBossAttackKeyframe(30, new Vec3(0, 0, .45), new EnderBossAttackKeyframe.SwingData(false, false)),
                                        new EnderBossAttackKeyframe(50, new Vec3(0, 0.1, 1.25), new Vec3(0, .3, 0.8), new EnderBossAttackKeyframe.SwingData(false, false))
                                ).build(),
                        AttackAnimationData.builder("scythe_jump_combo")
                                .length(45)
                                .cancellable()
                                .rangeMultiplier(3f)
                                .attacks(
                                        new EnderBossAttackKeyframe(20, new Vec3(0, 1, 0), new Vec3(0, 1.15, .1), new EnderBossAttackKeyframe.SwingData(true, false)),
                                        new EnderBossAttackKeyframe(35, new Vec3(0, 0, -.2), new Vec3(0, 0, 0.5), new EnderBossAttackKeyframe.SwingData(false, false))
                                ).build(),
                        AttackAnimationData.builder("scythe_downslash_sideslash")
                                .length(60)
                                .attacks(
                                        new EnderBossAttackKeyframe(22, new Vec3(0, 0, .5f), new Vec3(0, -.2, 0), new EnderBossAttackKeyframe.SwingData(true, true)),
                                        new EnderBossAttackKeyframe(40, new Vec3(0, .1, 0.8), new EnderBossAttackKeyframe.SwingData(false, false))
                                ).build(),
                        AttackAnimationData.builder("scythe_horizontal_slash_spin")
                                .length(45)
                                .area(0.25f)
                                .rangeMultiplier(3f)
                                .attacks(
                                        new EnderBossAttackKeyframe(14, new Vec3(0, 0.1, 1.25), new Vec3(0, .1, 0.8), new EnderBossAttackKeyframe.SwingData(false, true)),
                                        new EnderBossAttackKeyframe(30, new Vec3(0, 0.1, 1.85), new Vec3(0, .3, 0.8), new EnderBossAttackKeyframe.SwingData(false, false))
                                ).build()

                ))
                .setComboChance(1f)
                .setMeleeAttackInverval(10, 30)
                .setMeleeBias(1f, 1f)
                .setSpells(
                        List.of(SpellRegistry.MAGIC_MISSILE_SPELL.get(), SpellRegistry.ELDRITCH_BLAST_SPELL.get(), SpellRegistry.BLOOD_SLASH_SPELL.get(), SpellRegistry.FIREBOLT_SPELL.get(), SpellRegistry.WISP_SPELL.get(), SpellRegistry.BALL_LIGHTNING_SPELL.get()),
                        List.of(SpellRegistry.FANG_WARD_SPELL.get(), SpellRegistry.FROSTWAVE_SPELL.get(), SpellRegistry.EARTHQUAKE_SPELL.get()),
                        List.of(SpellRegistry.BURNING_DASH_SPELL.get()),
                        List.of(SpellRegistry.SUMMON_SWORDS.get(), SpellRegistry.CLEANSE_SPELL.get())
                );
        if(!isClone){
            this.goalSelector.addGoal(2, new EnderDaggerSwarmAbilityGoal(this));
            this.goalSelector.addGoal(2, new EnderDaggerZoneAbilityGoal(this));
        }
        this.goalSelector.addGoal(3, attackGoal);

        this.goalSelector.addGoal(4, new PatrolNearLocationGoal(this, 30, .75f));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.targetSelector.addGoal(1, new MomentHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    private final ServerBossEvent bossEvent = (ServerBossEvent) (new ServerBossEvent(Component.literal("Dark Doppelganger"), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS)).setCreateWorldFog(true);

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    private static final AttributeModifier MANA_MODIFIER = new AttributeModifier(ResourceLocation.fromNamespaceAndPath(DarkDoppelgangerMod.MOD_ID, "mana"), 10000, AttributeModifier.Operation.ADD_VALUE);

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData) {
        super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData);
        RandomSource randomsource = Utils.random;
        this.populateDefaultEquipmentSlots(randomsource, pDifficulty);
        this.setLeftHanded(false);
        this.getAttribute(AttributeRegistry.MAX_MANA).addOrReplacePermanentModifier(MANA_MODIFIER);
        this.playerScale = pLevel.players().stream().filter(player -> distanceToSqr(player) < 3600 && !player.isSpectator() && !player.isCreative()).toList().size();
        this.setHealth(this.getMaxHealth());
        return pSpawnData;
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
            summonIllusionClones();
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
            summonIllusionClones();
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

        float maxHealth = this.getMaxHealth();
        float currentHealth = this.getHealth();
        this.bossEvent.setProgress(currentHealth / maxHealth);
        if (daggerTime > 0) {
            daggerTime--;
        }
        if (destroyBlockDelay > 0) {
            --destroyBlockDelay;
        }
    }

    int daggerTime;
    public boolean clientDaggerParticles;

    public void procSpectralDagger() {
        if (!level().isClientSide) {
            serverTriggerEvent(PROC_SPECTRAL_DAGGER);
        } else {
            clientDaggerParticles = true;
        }
        this.daggerTime = 15;
    }

    public boolean spectralDaggerActive() {
        return daggerTime > 0;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (tickCount > 400 && this.getTarget() == null && this.tickCount - this.getLastHurtByMobTimestamp() > 200) {
            if (tickCount % 20 == 0) {
                this.heal(5);
            }
        }
    }

    @Override
    public boolean causeFallDamage(float pFallDistance, float pMultiplier, DamageSource pSource) {
        return false;
    }

    public static AttributeSupplier.Builder prepareAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.ATTACK_DAMAGE, 20.0)
                .add(AttributeRegistry.SPELL_POWER, 1.25)
                .add(Attributes.ARMOR, 20.0)
                .add(AttributeRegistry.SPELL_RESIST, 1.25)
                .add(AttributeRegistry.FIRE_MAGIC_RESIST, 1.5)
                .add(Attributes.MAX_HEALTH, 6000.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.ATTACK_KNOCKBACK, .6)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.SCALE, 1)
                .add(Attributes.GRAVITY, 0.03)
                .add(Attributes.ENTITY_INTERACTION_RANGE, 3)
                .add(Attributes.STEP_HEIGHT, 1)
                .add(Attributes.MOVEMENT_SPEED, .21);
    }

    @Override
    public void calculateEntityAnimation(boolean pIncludeHeight) {
        super.calculateEntityAnimation(false);
    }

    @Override
    protected void updateWalkAnimation(float f) {
        //reduce walk animation swing if we are floating or meleeing
        super.updateWalkAnimation(f * (!this.onGround() ? .5f : .9f));
    }

    @Override
    public boolean bobBodyWhileWalking() {
        return !isAnimating();
    }

    //ANIMATIONS
    RawAnimation animationToPlay = null;
    private final RawAnimation ANIMATION_SPAWN = RawAnimation.begin().thenPlay("join_1");
    private final AnimationController<DarkDoppelgangerEntity> meleeController = new AnimationController<>(this, "melee_animations", 0, this::predicate);
    private final AnimationController<DarkDoppelgangerEntity> spawnController = new AnimationController<>(this, "spawn_animations", 0, this::spawnPredicate);

    @Override
    public void playAnimation(String animationId) {
        animationToPlay = RawAnimation.begin().thenPlay(animationId);
        canAnimateOver = animationId.equals("summon_fiery_daggers");
    }

    private PlayState predicate(AnimationState<DarkDoppelgangerEntity> animationEvent) {
        var controller = animationEvent.getController();

        if (age > 45 && this.animationToPlay != null) {
            controller.forceAnimationReset();
            controller.setAnimation(animationToPlay);
            animationToPlay = null;
        }
        return PlayState.CONTINUE;
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
        return (meleeController.getAnimationState() == AnimationController.State.RUNNING && !canAnimateOver) || super.isAnimating();
    }

    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        if (level().isClientSide) {
            return false;
        }
        /*
        can parry:
        - serverside
        - in combat
        - we aren't in melee attack anim or spell cast
        - the damage source is caused by an entity (ie not fall damage)
        - the damage is caused within our rough field of vision (117 degrees)
        - the damage is not /kill
         */
        boolean canParry = this.isAggressive() &&
                !isImmobile() &&
                !attackGoal.isActing() &&
                pSource.getEntity() != null &&
                pSource.getSourcePosition() != null && pSource.getSourcePosition().subtract(this.position()).normalize().dot(this.getForward()) >= 0.35
                && !pSource.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
        if (canParry && this.random.nextFloat() < 0.5) {
            serverTriggerAnimation("offhand_parry");
            procSpectralDagger();
            this.playSound(SoundRegistry.FIRE_DAGGER_PARRY.get());
            return false;
        }
        // damage limiter
        var limit = getMaxHealth() * 0.025f;
        if (pAmount > limit) {
            pAmount = limit + (pAmount - limit) * .3f; // damage about limit has .3x multiplier applied
        }
        if (pSource.is(DamageTypes.IN_WALL) && this.destroyBlockDelay <= 0) {
            Utils.doMobBreakSuffocatingBlocks(this);
            destroyBlockDelay = 40;
        }

        if (this.isDeadOrDying() || pSource == this.level().damageSources().fellOutOfWorld()) {
            return false;
        }
        if (isClone) {
            return super.hurt(pSource, pAmount);
        }
        float newHealth = this.getHealth() - pAmount;

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

        Entity attacker = pSource.getEntity();
        if (attacker instanceof LivingEntity && attacker != this) {
            this.setTarget((LivingEntity) attacker);
        }

        return super.hurt(pSource, pAmount);
    }

    protected void setSecondPhaseGoals() {
        this.attackGoal = (EnderBossAttackGoal) new EnderBossAttackGoal(this, 1.5f, 50, 75)
                .setMoveset(List.of(
                        AttackAnimationData.builder("scythe_dagger_double_horizontal")
                                .length(60)
                                .attacks(
                                        new EnderBossAttackKeyframe(15, new Vec3(0, 0, .25), new EnderBossAttackKeyframe.SwingData(false, true)),
                                        new InvokeDaggerKeyframe(35),
                                        new EnderBossAttackKeyframe(36, new Vec3(0, 0, .75), new EnderBossAttackKeyframe.SwingData(false, false)),
                                        new AttackKeyframe(42, new Vec3(0, 0, 0))
                                ).build(),
                        AttackAnimationData.builder("scythe_backpedal")
                                .length(40)
                                .rangeMultiplier(2f)
                                .attacks(
                                        new EnderBossAttackKeyframe(20, new Vec3(0, .3, -2), new EnderBossAttackKeyframe.SwingData(false, true))
                                ).build(),
                        AttackAnimationData.builder("scythe_sideslash_downslash_sideslash")
                                .length(62)
                                .rangeMultiplier(2f)
                                .attacks(
                                        new EnderBossAttackKeyframe(18, new Vec3(0, 0, .45), new EnderBossAttackKeyframe.SwingData(false, true)),
                                        new EnderBossAttackKeyframe(30, new Vec3(0, 0, .45), new EnderBossAttackKeyframe.SwingData(false, false)),
                                        new EnderBossAttackKeyframe(50, new Vec3(0, 0.1, 1.25), new Vec3(0, .3, 0.8), new EnderBossAttackKeyframe.SwingData(false, false))
                                ).build(),
                        AttackAnimationData.builder("scythe_jump_combo")
                                .length(45)
                                .cancellable()
                                .rangeMultiplier(3f)
                                .attacks(
                                        new EnderBossAttackKeyframe(20, new Vec3(0, 1, 0), new Vec3(0, 1.15, .1), new EnderBossAttackKeyframe.SwingData(true, false)),
                                        new EnderBossAttackKeyframe(35, new Vec3(0, 0, -.2), new Vec3(0, 0, 0.5), new EnderBossAttackKeyframe.SwingData(false, false))
                                ).build(),
                        AttackAnimationData.builder("scythe_downslash_sideslash")
                                .length(60)
                                .attacks(
                                        new EnderBossAttackKeyframe(22, new Vec3(0, 0, .5f), new Vec3(0, -.2, 0), new EnderBossAttackKeyframe.SwingData(true, true)),
                                        new EnderBossAttackKeyframe(40, new Vec3(0, .1, 0.8), new EnderBossAttackKeyframe.SwingData(false, false))
                                ).build(),
                        AttackAnimationData.builder("scythe_horizontal_slash_spin")
                                .length(45)
                                .area(0.25f)
                                .rangeMultiplier(3f)
                                .attacks(
                                        new EnderBossAttackKeyframe(14, new Vec3(0, 0.1, 1.25), new Vec3(0, .1, 0.8), new EnderBossAttackKeyframe.SwingData(false, true)),
                                        new EnderBossAttackKeyframe(30, new Vec3(0, 0.1, 1.85), new Vec3(0, .3, 0.8), new EnderBossAttackKeyframe.SwingData(false, false))
                                ).build()

                ))
                .setComboChance(1f)
                .setMeleeAttackInverval(10, 30)
                .setMeleeBias(1f, 1f)
                .setSpells(
                        List.of(SpellRegistry.MAGIC_ARROW_SPELL.get(), SpellRegistry.SONIC_BOOM_SPELL.get(), SpellRegistry.BLOOD_SLASH_SPELL.get(), SpellRegistry.FIRE_ARROW_SPELL.get(), SpellRegistry.BALL_LIGHTNING_SPELL.get(), SpellRegistry.FIREFLY_SWARM_SPELL.get()),
                        List.of(SpellRegistry.FANG_WARD_SPELL.get(), SpellRegistry.EARTHQUAKE_SPELL.get()),
                        List.of(SpellRegistry.BURNING_DASH_SPELL.get(), SpellRegistry.FROST_STEP_SPELL.get()),
                        List.of(SpellRegistry.SUMMON_SWORDS.get(), SpellRegistry.CLEANSE_SPELL.get(), SpellRegistry.CHARGE_SPELL.get())
                );
    }

    protected void setThirdPhaseGoals() {
        this.attackGoal = (EnderBossAttackGoal) new EnderBossAttackGoal(this, 1.5f, 50, 75)
                .setMoveset(List.of(
                        AttackAnimationData.builder("scythe_dagger_double_horizontal")
                                .length(60)
                                .attacks(
                                        new EnderBossAttackKeyframe(15, new Vec3(0, 0, .25), new EnderBossAttackKeyframe.SwingData(false, true)),
                                        new InvokeDaggerKeyframe(35),
                                        new EnderBossAttackKeyframe(36, new Vec3(0, 0, .75), new EnderBossAttackKeyframe.SwingData(false, false)),
                                        new AttackKeyframe(42, new Vec3(0, 0, 0))
                                ).build(),
                        AttackAnimationData.builder("scythe_backpedal")
                                .length(40)
                                .rangeMultiplier(2f)
                                .attacks(
                                        new EnderBossAttackKeyframe(20, new Vec3(0, .3, -2), new EnderBossAttackKeyframe.SwingData(false, true))
                                ).build(),
                        AttackAnimationData.builder("scythe_sideslash_downslash_sideslash")
                                .length(62)
                                .rangeMultiplier(2f)
                                .attacks(
                                        new EnderBossAttackKeyframe(18, new Vec3(0, 0, .45), new EnderBossAttackKeyframe.SwingData(false, true)),
                                        new EnderBossAttackKeyframe(30, new Vec3(0, 0, .45), new EnderBossAttackKeyframe.SwingData(false, false)),
                                        new EnderBossAttackKeyframe(50, new Vec3(0, 0.1, 1.25), new Vec3(0, .3, 0.8), new EnderBossAttackKeyframe.SwingData(false, false))
                                ).build(),
                        AttackAnimationData.builder("scythe_jump_combo")
                                .length(45)
                                .cancellable()
                                .rangeMultiplier(3f)
                                .attacks(
                                        new EnderBossAttackKeyframe(20, new Vec3(0, 1, 0), new Vec3(0, 1.15, .1), new EnderBossAttackKeyframe.SwingData(true, false)),
                                        new EnderBossAttackKeyframe(35, new Vec3(0, 0, -.2), new Vec3(0, 0, 0.5), new EnderBossAttackKeyframe.SwingData(false, false))
                                ).build(),
                        AttackAnimationData.builder("scythe_downslash_sideslash")
                                .length(60)
                                .attacks(
                                        new EnderBossAttackKeyframe(22, new Vec3(0, 0, .5f), new Vec3(0, -.2, 0), new EnderBossAttackKeyframe.SwingData(true, true)),
                                        new EnderBossAttackKeyframe(40, new Vec3(0, .1, 0.8), new EnderBossAttackKeyframe.SwingData(false, false))
                                ).build(),
                        AttackAnimationData.builder("scythe_horizontal_slash_spin")
                                .length(45)
                                .area(0.25f)
                                .rangeMultiplier(3f)
                                .attacks(
                                        new EnderBossAttackKeyframe(14, new Vec3(0, 0.1, 1.25), new Vec3(0, .1, 0.8), new EnderBossAttackKeyframe.SwingData(false, true)),
                                        new EnderBossAttackKeyframe(30, new Vec3(0, 0.1, 1.85), new Vec3(0, .3, 0.8), new EnderBossAttackKeyframe.SwingData(false, false))
                                ).build()

                ))
                .setComboChance(1f)
                .setMeleeAttackInverval(10, 30)
                .setMeleeBias(1f, 1f)
                .setSpells(
                        List.of(SpellRegistry.MAGIC_ARROW_SPELL.get(), SpellRegistry.SCULK_TENTACLES_SPELL.get(), SpellRegistry.FIREBALL_SPELL.get(), SpellRegistry.LIGHTNING_LANCE_SPELL.get(), SpellRegistry.RAY_OF_FROST_SPELL.get(), SpellRegistry.POISON_SPLASH_SPELL.get()),
                        List.of(SpellRegistry.EARTHQUAKE_SPELL.get(), SpellRegistry.HEAT_SURGE_SPELL.get(), SpellRegistry.SHOCKWAVE_SPELL.get()),
                        List.of(SpellRegistry.BURNING_DASH_SPELL.get(), SpellRegistry.BLOOD_STEP_SPELL.get()),
                        List.of(SpellRegistry.SUMMON_SWORDS.get(), SpellRegistry.CLEANSE_SPELL.get(), SpellRegistry.EVASION_SPELL.get())
                );
    }

    protected void setFinalPhaseGoals() {
        this.attackGoal = (EnderBossAttackGoal) new EnderBossAttackGoal(this, 1.7f, 50, 75)
                .setMoveset(List.of(
                        AttackAnimationData.builder("scythe_dagger_double_horizontal")
                                .length(60)
                                .attacks(
                                        new EnderBossAttackKeyframe(15, new Vec3(0, 0, .25), new EnderBossAttackKeyframe.SwingData(false, true)),
                                        new InvokeDaggerKeyframe(35),
                                        new EnderBossAttackKeyframe(36, new Vec3(0, 0, .75), new EnderBossAttackKeyframe.SwingData(false, false)),
                                        new AttackKeyframe(42, new Vec3(0, 0, 0))
                                ).build(),
                        AttackAnimationData.builder("scythe_backpedal")
                                .length(40)
                                .rangeMultiplier(2f)
                                .attacks(
                                        new EnderBossAttackKeyframe(20, new Vec3(0, .3, -2), new EnderBossAttackKeyframe.SwingData(false, true))
                                ).build(),
                        AttackAnimationData.builder("scythe_sideslash_downslash_sideslash")
                                .length(62)
                                .rangeMultiplier(2f)
                                .attacks(
                                        new EnderBossAttackKeyframe(18, new Vec3(0, 0, .45), new EnderBossAttackKeyframe.SwingData(false, true)),
                                        new EnderBossAttackKeyframe(30, new Vec3(0, 0, .45), new EnderBossAttackKeyframe.SwingData(false, false)),
                                        new EnderBossAttackKeyframe(50, new Vec3(0, 0.1, 1.25), new Vec3(0, .3, 0.8), new EnderBossAttackKeyframe.SwingData(false, false))
                                ).build(),
                        AttackAnimationData.builder("scythe_jump_combo")
                                .length(45)
                                .cancellable()
                                .rangeMultiplier(3f)
                                .attacks(
                                        new EnderBossAttackKeyframe(20, new Vec3(0, 1, 0), new Vec3(0, 1.15, .1), new EnderBossAttackKeyframe.SwingData(true, false)),
                                        new EnderBossAttackKeyframe(35, new Vec3(0, 0, -.2), new Vec3(0, 0, 0.5), new EnderBossAttackKeyframe.SwingData(false, false))
                                ).build(),
                        AttackAnimationData.builder("scythe_downslash_sideslash")
                                .length(60)
                                .attacks(
                                        new EnderBossAttackKeyframe(22, new Vec3(0, 0, .5f), new Vec3(0, -.2, 0), new EnderBossAttackKeyframe.SwingData(true, true)),
                                        new EnderBossAttackKeyframe(40, new Vec3(0, .1, 0.8), new EnderBossAttackKeyframe.SwingData(false, false))
                                ).build(),
                        AttackAnimationData.builder("scythe_horizontal_slash_spin")
                                .length(45)
                                .area(0.25f)
                                .rangeMultiplier(3f)
                                .attacks(
                                        new EnderBossAttackKeyframe(14, new Vec3(0, 0.1, 1.25), new Vec3(0, .1, 0.8), new EnderBossAttackKeyframe.SwingData(false, true)),
                                        new EnderBossAttackKeyframe(30, new Vec3(0, 0.1, 1.85), new Vec3(0, .3, 0.8), new EnderBossAttackKeyframe.SwingData(false, false))
                                ).build()

                ))
                .setComboChance(1f)
                .setMeleeAttackInverval(10, 30)
                .setMeleeBias(1f, 1f)
                .setSpells(
                        List.of(SpellRegistry.MAGIC_ARROW_SPELL.get(), SpellRegistry.SCULK_TENTACLES_SPELL.get(), SpellRegistry.FIREBALL_SPELL.get(), SpellRegistry.LIGHTNING_LANCE_SPELL.get(), SpellRegistry.RAY_OF_FROST_SPELL.get(), SpellRegistry.SONIC_BOOM_SPELL.get(), SpellRegistry.ICE_SPIKES_SPELL.get(), SpellRegistry.BALL_LIGHTNING_SPELL.get(), SpellRegistry.ACID_ORB_SPELL.get()),
                        List.of(SpellRegistry.EARTHQUAKE_SPELL.get(), SpellRegistry.HEAT_SURGE_SPELL.get(), SpellRegistry.SHOCKWAVE_SPELL.get(), SpellRegistry.RAISE_HELL_SPELL.get(), SpellRegistry.OAKSKIN_SPELL.get()),
                        List.of(SpellRegistry.BURNING_DASH_SPELL.get(), SpellRegistry.BLOOD_STEP_SPELL.get()),
                        List.of(SpellRegistry.SUMMON_SWORDS.get(), SpellRegistry.CLEANSE_SPELL.get(), SpellRegistry.ABYSSAL_SHROUD_SPELL.get(), SpellRegistry.HASTE_SPELL.get(), SpellRegistry.SLOW_SPELL.get(), SpellRegistry.BLIGHT_SPELL.get())
                );
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putInt("playerScale", playerScale);
        pCompound.putLong("unloadedGametime", level().getGameTime());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        this.playerScale = pCompound.getInt("playerScale");
    }

    @Override
    public void load(CompoundTag pCompound) {
        if (pCompound.contains("unloadedGametime", 99)) {
            var unloadTimestamp = pCompound.getLong("unloadedGametime");
            var delta = level().getGameTime() - unloadTimestamp;
            if (delta > UNLOADED_DESPAWN_LIMIT_SECONDS * 20) {
                this.setRemoved(RemovalReason.DISCARDED);
                return;
            }
        }
        super.load(pCompound);
    }

    @Override
    public boolean isAlliedTo(Entity pEntity) {
        return super.isAlliedTo(pEntity) || pEntity.getType().is(ModTags.CLONES);
    }

    @Override
    protected PathNavigation createNavigation(Level pLevel) {
        return new NotIdioticNavigation(this, pLevel);
    }
}