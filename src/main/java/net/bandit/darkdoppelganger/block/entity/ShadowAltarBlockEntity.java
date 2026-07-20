package net.bandit.darkdoppelganger.block.entity;

import net.bandit.darkdoppelganger.registry.ModBlockEntities;
import net.bandit.darkdoppelganger.registry.ModSounds;
import net.bandit.darkdoppelganger.summoning.DoppelgangerSummoning;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ShadowAltarBlockEntity extends BlockEntity {

    private static final String TAG_ORB = "ShadowOrb";
    private static final String TAG_SUMMONER = "Summoner";
    private static final String TAG_TICKS = "RitualTicks";
    private static final String TAG_ACTIVE = "RitualActive";
    private static final String TAG_RETURN_ORB = "ReturnOrb";

    private static final int RITUAL_DURATION_TICKS = 100;

    private ItemStack orb = ItemStack.EMPTY;
    @Nullable
    private UUID summonerUuid;
    private int ritualTicks;
    private boolean active;
    private boolean returnOrb;

    public ShadowAltarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SHADOW_ALTAR.get(), pos, state);
    }

    public boolean hasOrb() {
        return !orb.isEmpty();
    }

    public ItemStack getOrbForRender() {
        return orb;
    }

    public boolean isActive() {
        return active;
    }

    public void beginRitual(Player player, ItemStack heldOrb) {
        if (level == null || level.isClientSide || hasOrb()) {
            return;
        }

        orb = heldOrb.copy();
        orb.setCount(1);
        summonerUuid = player.getUUID();
        ritualTicks = RITUAL_DURATION_TICKS;
        active = true;
        returnOrb = !player.getAbilities().instabuild;

        if (returnOrb) {
            heldOrb.shrink(1);
        }

        player.displayClientMessage(
                Component.translatable("message.darkdoppelganger.shadow_altar.started"),
                true
        );

        level.playSound(
                null,
                worldPosition,
                ModSounds.BOSS_LAUGH.get(),
                SoundSource.HOSTILE,
                1.5F,
                0.8F
        );
        setChangedAndSync();
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            ShadowAltarBlockEntity altar
    ) {
        if (!(level instanceof ServerLevel serverLevel) || !altar.active || altar.orb.isEmpty()) {
            return;
        }

        altar.ritualTicks--;
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 1.58;
        double z = pos.getZ() + 0.5;

        if ((altar.ritualTicks & 1) == 0) {
            serverLevel.sendParticles(
                    ParticleTypes.SMOKE,
                    x, y, z,
                    2, 0.18, 0.08, 0.18, 0.0
            );
        }
        if (altar.ritualTicks % 4 == 0) {
            serverLevel.sendParticles(
                    ParticleTypes.PORTAL,
                    x, y + 0.1, z,
                    3, 0.22, 0.14, 0.22, 0.02
            );
        }
        if (altar.ritualTicks > 0 && altar.ritualTicks % 20 == 0) {
            float pitch = 0.7F + ((RITUAL_DURATION_TICKS - altar.ritualTicks) / 100.0F);
            serverLevel.playSound(
                    null,
                    pos,
                    SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.BLOCKS,
                    0.6F,
                    pitch
            );
        }

        if (altar.ritualTicks > 0) {
            altar.setChanged();
            return;
        }

        altar.finishRitual(serverLevel);
    }

    private void finishRitual(ServerLevel level) {
        ServerPlayer summoner = summonerUuid == null
                ? null
                : level.getServer().getPlayerList().getPlayer(summonerUuid);

        boolean validSummoner = summoner != null
                && summoner.isAlive()
                && summoner.level() == level;

        if (!validSummoner) {
            failRitual(level, summoner);
            return;
        }

        boolean summoned = DoppelgangerSummoning.summon(level, summoner, worldPosition);
        if (!summoned) {
            failRitual(level, summoner);
            return;
        }

        double x = worldPosition.getX() + 0.5;
        double y = worldPosition.getY() + 1.6;
        double z = worldPosition.getZ() + 0.5;
        level.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
        level.sendParticles(ParticleTypes.PORTAL, x, y, z, 60, 0.5, 0.5, 0.5, 0.2);
        level.playSound(null, worldPosition, SoundEvents.ENDERMAN_SCREAM, SoundSource.HOSTILE, 0.8F, 0.7F);

        clearRitual();
    }

    private void failRitual(ServerLevel level, @Nullable ServerPlayer summoner) {
        if (summoner != null) {
            summoner.displayClientMessage(
                    Component.translatable("message.darkdoppelganger.shadow_altar.failed"),
                    true
            );
        }

        level.playSound(
                null,
                worldPosition,
                SoundEvents.FIRE_EXTINGUISH,
                SoundSource.BLOCKS,
                0.8F,
                0.7F
        );
        dropStoredOrb();
    }

    public void dropStoredOrb() {
        if (level == null || level.isClientSide || orb.isEmpty()) {
            return;
        }

        if (returnOrb) {
            Containers.dropItemStack(
                    level,
                    worldPosition.getX() + 0.5,
                    worldPosition.getY() + 1.5,
                    worldPosition.getZ() + 0.5,
                    orb.copy()
            );
        }

        orb = ItemStack.EMPTY;
        active = false;
        ritualTicks = 0;
        summonerUuid = null;
        returnOrb = false;
        setChangedAndSync();
    }

    private void clearRitual() {
        orb = ItemStack.EMPTY;
        active = false;
        ritualTicks = 0;
        summonerUuid = null;
        returnOrb = false;
        setChangedAndSync();
    }

    private void setChangedAndSync() {
        setChanged();
        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!orb.isEmpty()) {
            tag.put(TAG_ORB, orb.save(new CompoundTag()));
        }
        if (summonerUuid != null) {
            tag.putUUID(TAG_SUMMONER, summonerUuid);
        }
        tag.putInt(TAG_TICKS, ritualTicks);
        tag.putBoolean(TAG_ACTIVE, active);
        tag.putBoolean(TAG_RETURN_ORB, returnOrb);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        orb = tag.contains(TAG_ORB) ? ItemStack.of(tag.getCompound(TAG_ORB)) : ItemStack.EMPTY;
        summonerUuid = tag.hasUUID(TAG_SUMMONER) ? tag.getUUID(TAG_SUMMONER) : null;
        ritualTicks = tag.getInt(TAG_TICKS);
        active = tag.getBoolean(TAG_ACTIVE);
        returnOrb = tag.getBoolean(TAG_RETURN_ORB);
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }
}
