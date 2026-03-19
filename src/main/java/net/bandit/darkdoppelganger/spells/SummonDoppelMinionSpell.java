package net.bandit.darkdoppelganger.spells;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.bandit.darkdoppelganger.DarkDoppelgangerMod;
import net.bandit.darkdoppelganger.entity.DarkDoppelgangerEquipmentHelper;
import net.bandit.darkdoppelganger.entity.DarkDoppelgangerMinionEntity;
import net.bandit.darkdoppelganger.entity.EntityRegistry;
import net.bandit.darkdoppelganger.entity.PortalJoinEntity;
import net.bandit.darkdoppelganger.registry.ModAnimations;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

@AutoSpellConfig
public class SummonDoppelMinionSpell extends AbstractSpell {

    private static final String NBT_PLAYER_MINION_UUID = "DarkDoppel_SummonerMinionUUID";
    private static final String PERSISTED_TAG = "darkdoppelganger";
    private static final String NBT_WARN_COOLDOWN = "MinionWarnCooldown";
    private static final int WARN_COOLDOWN_TICKS = 40;

    private final ResourceLocation spellId =
            ResourceLocation.fromNamespaceAndPath(DarkDoppelgangerMod.MOD_ID, "summon_doppel_minion");

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
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public CastType getCastType() {
        return CastType.LONG;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
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
        return ModAnimations.PLAYER_JOIN;
    }

    @Override
    public boolean allowCrafting() {
        return true;
    }

    @Override
    public boolean allowLooting() {
        return true;
    }

    @Override
    public void onServerCastComplete(Level level, int spellLevel, LivingEntity caster, MagicData magicData, boolean cancelled) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (!(caster instanceof ServerPlayer player)) return;

        if (cancelled) {
            magicData.resetCastingState();
            player.stopUsingItem();
            super.onServerCastComplete(level, spellLevel, caster, magicData, true);
            return;
        }

        if (hasLivingMinion(serverLevel, player)) {
            warnOnce(player, Component.literal("You already have a minion summoned."));
            magicData.resetCastingState();
            player.stopUsingItem();
            super.onServerCastComplete(level, spellLevel, caster, magicData, true);
            return;
        }

        Vec3 spawnPos = findSafeSpawnLocation(serverLevel, caster, 4.0f);

        PortalJoinEntity portal = new PortalJoinEntity(EntityRegistry.PORTAL_JOIN_ENTITY.get(), serverLevel);
        portal.setPos(spawnPos);
        portal.setYRot(caster.getYRot());
        portal.yRotO = caster.getYRot();
        serverLevel.addFreshEntity(portal);

        DarkDoppelgangerMinionEntity minion = EntityRegistry.DARK_DOPPELGANGER_MINION.get().create(serverLevel);
        if (minion == null) return;

        minion.setPos(spawnPos);
        minion.setYRot(caster.getYRot());
        minion.yRotO = caster.getYRot();

        minion.setSummonerUUID(player.getUUID());
        minion.setBossMinion(false);
        minion.setUseSummonerSkin(true);
        minion.setSkinPlayerUUID(player.getUUID());
        minion.setCustomName(Component.literal(player.getGameProfile().getName() + "'s Minion"));
        minion.setCustomNameVisible(true);

        DarkDoppelgangerEquipmentHelper.applyPlayerLoadout(minion, player);

        serverLevel.addFreshEntity(minion);
        setStoredMinionUUID(player, minion.getUUID());

        super.onServerCastComplete(level, spellLevel, caster, magicData, false);
    }

    private static boolean hasLivingMinion(ServerLevel level, ServerPlayer player) {
        UUID uuid = getStoredMinionUUID(player);
        if (uuid == null) return false;

        Entity e = level.getEntity(uuid);
        if (e instanceof DarkDoppelgangerMinionEntity minion
                && minion.isAlive()
                && !minion.isBossMinion()
                && player.getUUID().equals(minion.getSummonerUUID())) {
            return true;
        }

        setStoredMinionUUID(player, null);
        return false;
    }

    private static @Nullable UUID getStoredMinionUUID(ServerPlayer player) {
        CompoundTag tag = player.getPersistentData();
        return tag.hasUUID(NBT_PLAYER_MINION_UUID) ? tag.getUUID(NBT_PLAYER_MINION_UUID) : null;
    }

    private static void setStoredMinionUUID(ServerPlayer player, @Nullable UUID uuid) {
        CompoundTag tag = player.getPersistentData();
        if (uuid == null) tag.remove(NBT_PLAYER_MINION_UUID);
        else tag.putUUID(NBT_PLAYER_MINION_UUID, uuid);
    }

    private static void warnOnce(ServerPlayer player, Component msg) {
        CompoundTag root = player.getPersistentData();
        CompoundTag data = root.getCompound(PERSISTED_TAG);

        int cd = data.getInt(NBT_WARN_COOLDOWN);
        if (cd <= 0) {
            player.sendSystemMessage(msg);
            data.putInt(NBT_WARN_COOLDOWN, WARN_COOLDOWN_TICKS);
            root.put(PERSISTED_TAG, data);
        }
    }

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