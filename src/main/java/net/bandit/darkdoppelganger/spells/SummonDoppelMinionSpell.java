package net.bandit.darkdoppelganger.spells;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.capabilities.magic.*;
import net.bandit.darkdoppelganger.DarkDoppelgangerMod;
import net.bandit.darkdoppelganger.entity.DarkDoppelgangerMinionEntity;
import net.bandit.darkdoppelganger.entity.PortalJoinEntity;
import net.bandit.darkdoppelganger.registry.AnimationsRegistry;
import net.bandit.darkdoppelganger.registry.EntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class SummonDoppelMinionSpell extends AbstractSpell {

    private final ResourceLocation spellId =
            ResourceLocation.fromNamespaceAndPath(DarkDoppelgangerMod.MOD_ID, "summon_doppel_minion");

    private static final int SUMMON_TIME_TICKS = 20 * 60 * 5;

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(SchoolRegistry.ENDER_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(25)
            .setAllowCrafting(false)
            .build();

    public SummonDoppelMinionSpell() {
        this.baseManaCost = 35;
        this.manaCostPerLevel = 0;

        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 0;

        this.castTime = 40;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public CastType getCastType() {
        return CastType.LONG;
    }

    @Override
    public int getCastTime(int spellLevel) {
        return castTime;
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
        return AnimationsRegistry.PLAYER_JOIN;
    }

    @Override
    public int getRecastCount(int spellLevel, @Nullable LivingEntity entity) {
        return 2;
    }

    @Override
    public void onRecastFinished(ServerPlayer serverPlayer,
                                 RecastInstance recastInstance,
                                 RecastResult recastResult,
                                 ICastDataSerializable castDataSerializable) {

        if (SummonManager.recastFinishedHelper(serverPlayer, recastInstance, recastResult, castDataSerializable)) {
            super.onRecastFinished(serverPlayer, recastInstance, recastResult, castDataSerializable);
        }
    }

    @Override
    public ICastDataSerializable getEmptyCastData() {
        return new SummonedEntitiesCastData();
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData magicData) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (!(entity instanceof ServerPlayer player)) return;

        PlayerRecasts recasts = magicData.getPlayerRecasts();

        if (recasts.hasRecastForSpell(this)) {
            return;
        }

        Vec3 spawnPos = findSafeSpawnLocation(serverLevel, entity, 4.0f);

        PortalJoinEntity portal = new PortalJoinEntity(EntityRegistry.PORTAL_JOIN_ENTITY.get(), serverLevel);
        portal.setPos(spawnPos);
        portal.setYRot(entity.getYRot());
        portal.yRotO = entity.getYRot();
        serverLevel.addFreshEntity(portal);

        DarkDoppelgangerMinionEntity minion = EntityRegistry.DARK_DOPPELGANGER_MINION.get().create(serverLevel);
        if (minion == null) return;

        minion.setPos(spawnPos);
        minion.setYRot(entity.getYRot());
        minion.yRotO = entity.getYRot();

        minion.setSummonerUUID(player.getUUID());
        minion.setBossMinion(false);
        minion.setCustomName(net.minecraft.network.chat.Component.literal(player.getGameProfile().getName() + "'s Minion"));
        minion.setCustomNameVisible(true);

        serverLevel.addFreshEntity(minion);

        // Track with SummonManager + attach to cast data
        SummonedEntitiesCastData castData = new SummonedEntitiesCastData();
        SummonManager.initSummon(entity, minion, SUMMON_TIME_TICKS, castData);

        // Start recast timer (so second cast unsummons)
        RecastInstance recastInstance = new RecastInstance(
                this.getSpellId(),
                spellLevel,
                getRecastCount(spellLevel, entity),
                SUMMON_TIME_TICKS,
                castSource,
                castData
        );
        recasts.addRecast(recastInstance, magicData);
    }

    // --- Spawn helper (same idea as your old version) ---

    static Vec3 findSafeSpawnLocation(ServerLevel level, LivingEntity caster, float maxDistance) {
        var hit = Utils.getTargetBlock(level, caster, ClipContext.Fluid.NONE, maxDistance);
        BlockPos base = hit.getBlockPos();

        int radius = 2;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                BlockPos candidate = base.offset(dx, 0, dz);
                for (int dy = 2; dy >= -2; dy--) {
                    BlockPos feet = candidate.offset(0, dy, 0);
                    if (isStandable(level, feet)) {
                        return Vec3.atBottomCenterOf(feet).add(0, 0.01, 0);
                    }
                }
            }
        }

        Vec3 forward = caster.getForward().normalize().scale(1.5);
        Vec3 fallback = caster.position().add(forward);
        return new Vec3(fallback.x, caster.getY(), fallback.z);
    }

    private static boolean isStandable(ServerLevel level, BlockPos feetPos) {
        BlockPos below = feetPos.below();
        if (level.getBlockState(below).isAir()) return false;
        if (!level.getBlockState(feetPos).isAir()) return false;
        if (!level.getBlockState(feetPos.above()).isAir()) return false;
        return true;
    }
}
