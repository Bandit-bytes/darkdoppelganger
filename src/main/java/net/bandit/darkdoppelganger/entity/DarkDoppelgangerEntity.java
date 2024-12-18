package net.bandit.darkdoppelganger.entity;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.entity.mobs.IAnimatedAttacker;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.goals.AttackAnimationData;
import io.redspace.ironsspellbooks.entity.mobs.goals.PatrolNearLocationGoal;
import io.redspace.ironsspellbooks.entity.mobs.goals.SpellBarrageGoal;
import io.redspace.ironsspellbooks.entity.mobs.wizards.GenericAnimatedWarlockAttackGoal;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import net.bandit.darkdoppelganger.Config;
import net.bandit.darkdoppelganger.DarkDoppelgangerMod;
import net.bandit.darkdoppelganger.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

import java.util.List;


public class DarkDoppelgangerEntity extends AbstractSpellCastingMob implements Enemy, IAnimatedAttacker {

    private Player summonerPlayer;
    private final ServerBossEvent bossEvent;
    private boolean secondPhaseTriggered = false;
    private boolean thirdPhaseTriggered = false;
    public boolean isClone = false;
    private boolean musicPlaying = false;
    private int teleportCooldown = 80;
    private int shockwaveCooldown = 200;
    private int minionSummonCooldown = 300;
    private int lifeDrainCooldown = 150;
    private int roarSoundCooldown = 800;
    private int laughSoundCooldown = 800;
    private int meteorShowerCooldown = 400;
    private int levitationCooldown = 500;
    private int gravityPullCooldown = 300;
    private static final int MAX_MINIONS = 5;
    private static int currentMinionCount = 0;
    private int laughCooldown = 800;



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

            if(Config.DOPPLEGANGER_HARD_MODE.get()){
                this.getAttribute(AttributeRegistry.HOLY_MAGIC_RESIST.get()).setBaseValue(1.3f);
                this.getAttribute(AttributeRegistry.FIRE_MAGIC_RESIST.get()).setBaseValue(1.4f);
                this.getAttribute(AttributeRegistry.BLOOD_MAGIC_RESIST.get()).setBaseValue(1.4f);
                this.getAttribute(AttributeRegistry.NATURE_MAGIC_RESIST.get()).setBaseValue(1.3f);
                this.getAttribute(AttributeRegistry.ELDRITCH_MAGIC_RESIST.get()).setBaseValue(1.5f);
                this.getAttribute(AttributeRegistry.ICE_MAGIC_RESIST.get()).setBaseValue(1.4f);
                this.getAttribute(AttributeRegistry.LIGHTNING_MAGIC_RESIST.get()).setBaseValue(1.3f);
                this.getAttribute(AttributeRegistry.EVOCATION_MAGIC_RESIST.get()).setBaseValue(1.2f);
                this.getAttribute(AttributeRegistry.ENDER_MAGIC_RESIST.get()).setBaseValue(1.3f);
                this.getAttribute(AttributeRegistry.SPELL_RESIST.get()).setBaseValue(1.5f);
            }
        }
    }

    @Override
    protected void registerGoals() {
        setFirstPhaseGoals();
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    protected void setFirstPhaseGoals(){
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
                        List.of(SpellRegistry.BLIGHT_SPELL.get())
                )
        );
        this.goalSelector.addGoal(4, new PatrolNearLocationGoal(this, 30, .75f));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
    }

    protected void setSecondPhaseGoals(){
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

    protected void setThirdPhaseGoals(){
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

    protected void setFinalPhaseGoals(){
        this.goalSelector.getRunningGoals().forEach(WrappedGoal::stop);
        this.goalSelector.removeAllGoals((x) -> true);
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new SpellBarrageGoal(this, SpellRegistry.SCULK_TENTACLES_SPELL.get(), 3, 4, 100, 160, 1));
        this.goalSelector.addGoal(3, new GenericAnimatedWarlockAttackGoal<>(this, 1.4f, 30, 50, 3f)
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
                        List.of(SpellRegistry.ASCENSION_SPELL.get()),
                        List.of(SpellRegistry.BLOOD_STEP_SPELL.get(), SpellRegistry.ABYSSAL_SHROUD_SPELL.get()),
                        List.of(SpellRegistry.ABYSSAL_SHROUD_SPELL.get(), SpellRegistry.ECHOING_STRIKES_SPELL.get(), SpellRegistry.ROOT_SPELL.get(), SpellRegistry.BLIGHT_SPELL.get())
                )
        );
        this.goalSelector.addGoal(5, new PatrolNearLocationGoal(this, 30, .75f));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
    }

    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        this.setPersistenceRequired();

        if (!this.level().isClientSide && !this.isClone) {
            // Ensure all music is stopped first
            stopAllMusic();

            // Play the boss music only after stopping all other sounds
            if (!musicPlaying) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        ModSounds.BOSS_FIGHT_MUSIC.get(), SoundSource.MUSIC, 1.0F, 1.0F);
                musicPlaying = true;
            }
            adjustAttributesFromConfig();
        } else {
            spawnSummoningParticles();
        }
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

        // Sync health attribute with current health
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
            level().getServer().getPlayerList().getPlayers().forEach(player -> {
                player.connection.send(new ClientboundStopSoundPacket(null, SoundSource.MUSIC));
            });
        }
    }
    private void stopMinecraftAmbientMusic() {
        if (!level().isClientSide && level().getServer() != null) {
            // Loop through all players on the server
            for (ServerPlayer player : level().getServer().getPlayerList().getPlayers()) {
                // Stop specific Minecraft ambient music tracks
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
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        if (!this.isClone) {
            this.bossEvent.addPlayer(player);
        }
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        if (!this.isClone) {
            this.bossEvent.removePlayer(player);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (isClone) return;

        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());

        if (musicPlaying) {
            stopMinecraftAmbientMusic();
        }
        if (!isClone && laughCooldown > 0) {
            laughCooldown--;
        }

        if (this.getHealth() < this.getMaxHealth() * 0.3 && minionSummonCooldown <= 0) {
            summonIllusionClones();
            minionSummonCooldown = 400; // Reset cooldown
        }

        // Phase triggers
        if (!secondPhaseTriggered && this.getHealth() < this.getMaxHealth() * 0.2) {
            triggerSecondPhase();
            if(Config.DOPPLEGANGER_HARD_MODE.get()){
                setThirdPhaseGoals();
            } else {
                setSecondPhaseGoals();
            }
        }
        if (!thirdPhaseTriggered && this.getHealth() < this.getMaxHealth() * 0.2) {
            triggerThirdPhase();
            if(Config.DOPPLEGANGER_HARD_MODE.get()){
                setFinalPhaseGoals();
            } else {
                setThirdPhaseGoals();
            }
        }

        if(Config.DOPPLEGANGER_HARD_MODE.get()){
            this.addEffect(new MobEffectInstance(MobEffectRegistry.OAKSKIN.get(), 10, 8, false, false, true));
            this.addEffect(new MobEffectInstance(MobEffectRegistry.CHARGED.get(), 10, 2, false, false, true));
            this.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 10, 0, false, false));
        }

        if(Config.DOPPLEGANGER_HARD_MODE.get()){
            if(this.tickCount % 10 == 0){
                this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(20, 10, 20)).forEach(target -> {
                    if(target != this){
                        if(target.hasEffect(MobEffects.DIG_SPEED)){
                            target.removeEffect(MobEffects.DIG_SPEED);
                        }
                        if(target.hasEffect(MobEffectRegistry.ABYSSAL_SHROUD.get())){
                            target.removeEffect(MobEffectRegistry.ABYSSAL_SHROUD.get());
                        }
                        if(target.hasEffect(MobEffectRegistry.EVASION.get())){
                            target.removeEffect(MobEffectRegistry.EVASION.get());
                        }
                        if(target.hasEffect(MobEffectRegistry.HASTENED.get())){
                            target.removeEffect(MobEffectRegistry.HASTENED.get());
                        }
                        if(target.hasEffect(MobEffectRegistry.ECHOING_STRIKES.get())){
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

    }

    @Override
    public boolean addEffect(MobEffectInstance p_147208_, @Nullable Entity p_147209_) {
        if(!p_147208_.getEffect().isBeneficial()){
            return false;
        }
        return super.addEffect(p_147208_, p_147209_);
    }

    private void triggerSecondPhase() {
        secondPhaseTriggered = true;
        setHealth(getMaxHealth());

        for (ServerPlayer player : level().getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(50))) {
            player.sendSystemMessage(Component.literal("The Dark Doppelganger has entered its Second Phase!").withStyle(ChatFormatting.DARK_PURPLE));
            player.playSound(ModSounds.BOSS_ROAR.get(), 1.0F, 1.0F);
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
            player.playSound(ModSounds.BOSS_ROAR.get(), 1.0F, 1.0F);
            player.displayClientMessage(Component.literal("Final Form! Prepare yourself!").withStyle(ChatFormatting.RED), true);
        }

        for (Player player : level().players()) {
            player.getCooldowns().addCooldown(Items.TOTEM_OF_UNDYING, 60);
        }
        level().addParticle(ParticleTypes.DRAGON_BREATH, getX(), getY(), getZ(), 0, 0, 0);
//        createDangerZones();

        teleportCooldown = 40;
        shockwaveCooldown = 80;
        minionSummonCooldown = 150;
        lifeDrainCooldown = 200;
        meteorShowerCooldown = 200;
    }
//    private void createDangerZones() {
//        for (int i = 0; i < 3; i++) {
//            BlockPos dangerPos = new BlockPos((int) (getX() + random.nextInt(10) - 5), (int) getY(), (int) (getZ() + random.nextInt(10) - 5));
//            level().setBlock(dangerPos, Blocks.MAGMA_BLOCK.defaultBlockState(), 3);
//            level().addParticle(ParticleTypes.LAVA, dangerPos.getX(), dangerPos.getY(), dangerPos.getZ(), 0, 0.1, 0);
//        }
//    }
    private void meteorShowerAttack() {
        // Enhanced meteor shower with explosions, lightning, and fire
        for (int i = 0; i < 6; i++) {
            double xOffset = random.nextDouble() * 30 - 15;
            double zOffset = random.nextDouble() * 30 - 15;
            double targetX = getX() + xOffset;
            double targetZ = getZ() + zOffset;
            double targetY = level().getHeight() - 1;

            // Create a large explosion
            level().explode(null, targetX, targetY, targetZ, 4.0F, Level.ExplosionInteraction.BLOCK);
            level().playSound(null, targetX, targetY, targetZ, ModSounds.BOSS_ROAR.get(), SoundSource.HOSTILE, 1.0F, 0.8F);

            // Summon a lightning bolt
            LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level());
            if (lightning != null) {
                lightning.moveTo(targetX, targetY, targetZ);
                level().addFreshEntity(lightning);
            }

            // Set blocks on fire
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    if (level().isEmptyBlock(new BlockPos((int) (targetX + x), (int) targetY, (int) (targetZ + z)))) {
                        level().setBlock(new BlockPos((int) (targetX + x), (int) targetY, (int) (targetZ + z)), Blocks.FIRE.defaultBlockState(), 3);
                    }
                }
            }

            // Add particles
            level().addParticle(ParticleTypes.EXPLOSION, targetX, targetY, targetZ, 0, 0, 0);
            level().addParticle(ParticleTypes.SMOKE, targetX, targetY, targetZ, 0, 0.1, 0);
        }
        meteorShowerCooldown = 600; // Reset cooldown
    }

    private void levitationAttack() {
        if (getTarget() != null && !level().isClientSide) {
            // Boss levitates into the air
            teleportTo(getX(), getY() + 10, getZ());
            level().playSound(null, getX(), getY(), getZ(), ModSounds.BOSS_ROAR.get(), SoundSource.HOSTILE, 1.5F, 1.0F);

            // Apply levitation effect to nearby players
            level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(15)).forEach(player -> {
                player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 100, 1));
                player.hurt(level().damageSources().magic(), 5.0F);
            });

            // Shoot fireballs at players
            for (int i = 0; i < 3; i++) {
                if (getTarget() != null) {
                    LargeFireball fireball = new LargeFireball(level(), this, getTarget().getX() - getX(), getTarget().getY() - getY(), getTarget().getZ() - getZ(), 2);
                    fireball.setPos(getX(), getY() + 2, getZ());
                    level().addFreshEntity(fireball);
                }
            }
            levitationCooldown = 800; // Reset cooldown
        }
    }

    private void gravitationalPull() {
        // Enhanced gravitational pull with stronger force and particles
        level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(20)).forEach(player -> {
            double dx = getX() - player.getX();
            double dz = getZ() - player.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);
            double pullStrength = 1.2 / (distance + 0.1); // Stronger pull
            player.setDeltaMovement(dx * pullStrength, 0.5, dz * pullStrength);
            player.hurt(level().damageSources().magic(), 8.0F);

            // Add swirling particles
            level().addParticle(ParticleTypes.PORTAL, player.getX(), player.getY(), player.getZ(), 0, 1, 0);
        });

        level().playSound(null, getX(), getY(), getZ(), ModSounds.BOSS_ROAR.get(), SoundSource.HOSTILE, 1.0F, 0.5F);
        gravityPullCooldown = 400; // Reset cooldown
    }
    private void shadowTeleport() {
        if (getTarget() != null && !level().isClientSide) {
            teleportTo(getTarget().getX(), getTarget().getY(), getTarget().getZ());
            level().playSound(null, getX(), getY(), getZ(), ModSounds.BOSS_LAUGH.get(), SoundSource.HOSTILE, 0.0F, 1.0F);

            for (int i = 0; i < 10; i++) {
                level().addParticle(ParticleTypes.SMOKE, getX() + random.nextDouble() - 0.5, getY(), getZ() + random.nextDouble() - 0.5, 0, 0.1, 0);
            }

            laughSoundCooldown = 400;
        }
    }
    private void summonIllusionClones() {
        for (int i = 0; i < 3; i++) {
            DarkDoppelgangerEntity clone = EntityRegistry.DARK_DOPPELGANGER.get().create(level());
            if (clone != null) {
                clone.setPos(getX() + random.nextInt(5) - 2, getY(), getZ() + random.nextInt(5) - 2);
                clone.setHealth(10.0F); // Low health
                clone.isClone = true;
                clone.addTag("dark_doppelganger_clone");
                level().addFreshEntity(clone);

                level().addParticle(ParticleTypes.ENCHANT, clone.getX(), clone.getY(), clone.getZ(), 0, 1, 0);
            }
        }
    }


    private void shockwaveAttack() {
        level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(12)).forEach(player -> {
            double dx = player.getX() - this.getX();
            double dz = player.getZ() - this.getZ();
            player.knockback(2.0F, dx, dz);
            player.hurt(level().damageSources().mobAttack(this), 6.0F);
        });
        // Play roar sound only if cooldown is over
        if (roarSoundCooldown <= 0) {
            level().playSound(null, getX(), getY(), getZ(), ModSounds.BOSS_ROAR.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
            roarSoundCooldown = 400; // Reset cooldown
        }
    }

    private void summonMinions() {
        // Check if this is a clone or if the minion count has reached the limit
        if (isClone || currentMinionCount >= MAX_MINIONS) return;

        for (int i = 0; i < 2; i++) {
            if (currentMinionCount >= MAX_MINIONS) break; // Stop if max minions reached

            DarkDoppelgangerEntity minion = EntityRegistry.DARK_DOPPELGANGER.get().create(level());
            if (minion != null) {
                minion.setPos(getX() + random.nextInt(5) - 2, getY(), getZ() + random.nextInt(5) - 2);
                minion.setHealth(minion.getMaxHealth() * 0.3F);
                minion.isClone = true;
                level().addFreshEntity(minion);
                currentMinionCount++;
            }
        }
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
    public void die(DamageSource cause) {
        super.die(cause);
        if (isClone) {
            synchronized (DarkDoppelgangerEntity.class) {
                currentMinionCount = Math.max(0, currentMinionCount - 1);
            }
            return;
        }
        musicPlaying = false;

        if (!this.level().isClientSide) {
            if (cause.getEntity() instanceof ServerPlayer serverPlayer) {
                Advancement advancement = serverPlayer.getServer().getAdvancements()
                        .getAdvancement(new ResourceLocation("darkdoppelganger", "kill_dark_doppelganger"));

                if (advancement != null) {
                    serverPlayer.getAdvancements().award(advancement, "kill");
                    serverPlayer.sendSystemMessage(Component.literal("You have slain the Dark Doppelganger!"));
                }

                this.spawnAtLocation(Items.NETHER_STAR);
                this.spawnAtLocation(Items.ECHO_SHARD, 3);
                this.spawnAtLocation(Items.DIAMOND_BLOCK, 3);
                this.level().addFreshEntity(new ExperienceOrb(this.level(), this.getX(), this.getY(), this.getZ(), 2500));
            }

            // Stop the music for the real boss only
            this.level().getServer().getPlayerList().getPlayers().forEach(player -> {
                player.connection.send(new ClientboundStopSoundPacket(ModSounds.BOSS_FIGHT_MUSIC.get().getLocation(), SoundSource.MUSIC));
            });
            this.bossEvent.removeAllPlayers();
        }
    }
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 6000.0) // Default value
                .add(Attributes.ATTACK_DAMAGE, 20.0) // Default value
                .add(Attributes.MOVEMENT_SPEED, 0.36) // Default value
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6) // Default value
                .add(Attributes.ARMOR, 20.0) // Default value
                .add(Attributes.FOLLOW_RANGE, 64.0); // Default value
    }

//    @Override
//    public boolean shouldSheathSword() {
//        return true;
//    }

    RawAnimation animationToPlay = null;
    private final AnimationController<DarkDoppelgangerEntity> meleeController = new AnimationController<>(this, "keeper_animations", 0, this::predicate);

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

        if (this.animationToPlay != null) {
            controller.forceAnimationReset();
            controller.setAnimation(animationToPlay);
            animationToPlay = null;
        }
        return PlayState.CONTINUE;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(meleeController);
        super.registerControllers(controllerRegistrar);
    }

    @Override
    public boolean isAnimating() {
        return meleeController.getAnimationState() != AnimationController.State.STOPPED || super.isAnimating();
    }
}
