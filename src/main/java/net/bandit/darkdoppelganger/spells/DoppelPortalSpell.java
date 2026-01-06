package net.bandit.darkdoppelganger.spells;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.network.particles.TeleportParticlesPacket;
import io.redspace.ironsspellbooks.setup.Messages;
import net.bandit.darkdoppelganger.DarkDoppelgangerMod;
import net.bandit.darkdoppelganger.entity.EntityRegistry;
import net.bandit.darkdoppelganger.entity.PortalJoinEntity;
import net.bandit.darkdoppelganger.entity.PortalLeaveEntity;
import net.bandit.darkdoppelganger.registry.ModAnimations;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

@AutoSpellConfig
public class DoppelPortalSpell extends AbstractSpell {

    private final ResourceLocation spellId =
            ResourceLocation.fromNamespaceAndPath(DarkDoppelgangerMod.MOD_ID, "doppel_portal");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(SchoolRegistry.ENDER_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(8)
            .build();

    public DoppelPortalSpell() {
        // Feel-good player spell
        this.baseManaCost = 10;
        this.manaCostPerLevel = 0;

        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 0;

        this.castTime = 14;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public int getCastTime(int spellLevel) {
        return castTime;
    }

    @Override
    public CastType getCastType() {
        return CastType.CONTINUOUS;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.empty();
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.ENDERMAN_TELEPORT);
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return ModAnimations.PLAYER_JOIN;
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return ModAnimations.PLAYER_LEAVE;
    }

    @Override
    public boolean allowCrafting() {
        return false;
    }

    @Override
    public boolean allowLooting() {
        return false;
    }


    private static final String NBT_ROOT = "dd_doppel_portal";
    private static final String NBT_LEAVE_SPAWNED = "leaveSpawned";
    private static final String NBT_ORIGIN_X = "ox";
    private static final String NBT_ORIGIN_Y = "oy";
    private static final String NBT_ORIGIN_Z = "oz";
    private static final String NBT_ORIGIN_YAW = "oyaw";


    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData magicData) {
        if (!level.isClientSide) {
            var data = entity.getPersistentData().getCompound(NBT_ROOT);

            if (!data.getBoolean(NBT_LEAVE_SPAWNED)) {
                data.putBoolean(NBT_LEAVE_SPAWNED, true);
                data.putDouble(NBT_ORIGIN_X, entity.getX());
                data.putDouble(NBT_ORIGIN_Y, entity.getY());
                data.putDouble(NBT_ORIGIN_Z, entity.getZ());
                data.putFloat(NBT_ORIGIN_YAW, entity.getYRot());

                entity.getPersistentData().put(NBT_ROOT, data);

                PortalLeaveEntity portal = new PortalLeaveEntity(EntityRegistry.PORTAL_LEAVE_ENTITY.get(), level);
                portal.setYRot(entity.getYRot());
                portal.yRotO = entity.getYRot();
                portal.setPos(entity.position());
                level.addFreshEntity(portal);
            }
        }

        super.onCast(level, spellLevel, entity, castSource, magicData);
    }

    @Override
    public void onServerCastComplete(Level level, int spellLevel, LivingEntity entity, MagicData magicData, boolean cancelled) {
        try {
            if (cancelled) {
                return;
            }

            float maxDistance = getEvadeDistance(entity);

            Vec3 dest = findSafeTeleportLocation(level, entity, maxDistance);

            Messages.sendToPlayersTrackingEntity(new TeleportParticlesPacket(entity.position(), dest), entity, true);

            if (entity.isPassenger()) {
                entity.stopRiding();
            }

            entity.teleportTo(dest.x, dest.y, dest.z);
            entity.resetFallDistance();

            entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 25, 0, false, false, true));
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30, 0, false, false, true));

            getCastFinishSound().ifPresent(sound -> entity.playSound(sound, 2.0f, 1.0f));

            PortalJoinEntity portal = new PortalJoinEntity(EntityRegistry.PORTAL_JOIN_ENTITY.get(), level);
            portal.setYRot(entity.getYRot());
            portal.yRotO = entity.getYRot();
            portal.setPos(dest);
            level.addFreshEntity(portal);

        } finally {
            // Always clear our cast-state so next cast can spawn leave portal again
            entity.getPersistentData().remove(NBT_ROOT);
            super.onServerCastComplete(level, spellLevel, entity, magicData, cancelled);
        }
    }

    /**
     * Baseline ~16 blocks, scales up slightly with caster power, hard-clamped.
     * Makes it a legit "blink" for evasion.
     */
    private float getEvadeDistance(LivingEntity entity) {
        double mult = Utils.softCapFormula(getEntityPowerMultiplier(entity));
        double base = 16.0;
        double scaled = base * (0.9 + 0.25 * mult);
        return (float) Math.max(12.0, Math.min(26.0, scaled));
    }

    public static Vec3 findSafeTeleportLocation(Level level, LivingEntity entity, float maxDistance) {
        var hit = Utils.getTargetBlock(level, entity, ClipContext.Fluid.NONE, maxDistance);
        BlockPos base = hit.getBlockPos();

        // Scan around the hit for a standable spot
        int radius = 2;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                BlockPos candidate = base.offset(dx, 0, dz);

                // up first (ledges), then down
                for (int dy = 3; dy >= -3; dy--) {
                    BlockPos feet = candidate.offset(0, dy, 0);
                    if (isStandable(level, feet)) {
                        return Vec3.atBottomCenterOf(feet).add(0, 0.01, 0);
                    }
                }
            }
        }

        // Fallback: ray-based landing
        return findTeleportLocationFallback(level, entity, maxDistance);
    }

    private static boolean isStandable(Level level, BlockPos feetPos) {
        BlockPos below = feetPos.below();

        if (level.getBlockState(below).isAir()) return false;
        if (!level.getBlockState(feetPos).isAir()) return false;
        if (!level.getBlockState(feetPos.above()).isAir()) return false;

        return true;
    }

    /**
     * Kept close to your original "ledge / downcast" idea as a fallback.
     */
    public static Vec3 findTeleportLocationFallback(Level level, LivingEntity entity, float maxDistance) {
        var blockHitResult = Utils.getTargetBlock(level, entity, ClipContext.Fluid.NONE, maxDistance);
        var pos = blockHitResult.getBlockPos();

        Vec3 bbOffset = entity.getForward().normalize().multiply(entity.getBbWidth() / 3, 0, entity.getBbWidth() / 3);
        Vec3 bbImpact = blockHitResult.getLocation().subtract(bbOffset);

        int ledgeY = (int) level.clip(new ClipContext(
                Vec3.atBottomCenterOf(pos).add(0, 3, 0),
                Vec3.atBottomCenterOf(pos),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                null
        )).getLocation().y;

        BlockPos ledgePos = new BlockPos(pos.getX(), ledgeY, pos.getZ());
        boolean isAir = level.getBlockState(ledgePos).isAir();

        boolean los = level.clip(new ClipContext(
                bbImpact,
                bbImpact.add(0, ledgeY - pos.getY(), 0),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                entity
        )).getType() == HitResult.Type.MISS;

        if (isAir && los && Math.abs(ledgeY - pos.getY()) <= 3) {
            return new Vec3(pos.getX() + 0.5, ledgeY + 0.01, pos.getZ() + 0.5);
        }

        return level.clip(new ClipContext(
                bbImpact,
                bbImpact.add(0, -entity.getBbHeight(), 0),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                entity
        )).getLocation().add(0, 0.01, 0);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float dist = (caster != null) ? getEvadeDistance(caster) : 16.0f;
        return List.of(Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(dist, 1)));
    }
}
