package net.bandit.darkdoppelganger.block;

import com.mojang.serialization.MapCodec;
import net.bandit.darkdoppelganger.Config;
import net.bandit.darkdoppelganger.block.entity.ShadowAltarBlockEntity;
import net.bandit.darkdoppelganger.registry.ItemRegistry;
import net.bandit.darkdoppelganger.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class ShadowAltarBlock extends Block implements EntityBlock {
    public static final MapCodec<ShadowAltarBlock> CODEC = simpleCodec(ShadowAltarBlock::new);

    private static final VoxelShape SHAPE = Shapes.or(
            box(0.0, 0.0, 0.0, 16.0, 3.0, 16.0),
            box(3.0, 3.0, 3.0, 13.0, 18.0, 13.0),
            box(1.0, 18.0, 1.0, 15.0, 21.0, 15.0)
    );

    public ShadowAltarBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ShadowAltarBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type
    ) {
        if (level.isClientSide) {
            return null;
        }
        return type == ModBlockEntities.SHADOW_ALTAR.get()
                ? (level1, pos, state1, blockEntity) ->
                ShadowAltarBlockEntity.serverTick(level1, pos, state1, (ShadowAltarBlockEntity) blockEntity)
                : null;
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack held,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        if (!held.is(ItemRegistry.SHADOW_ORB.get())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof ShadowAltarBlockEntity altar)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (altar.hasOrb()) {
            if (!level.isClientSide) {
                player.displayClientMessage(
                        Component.translatable("message.darkdoppelganger.shadow_altar.occupied"),
                        true
                );
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        if (Config.ALTAR_END_ONLY.get() && !level.dimension().equals(Level.END)) {
            if (!level.isClientSide) {
                player.displayClientMessage(
                        Component.translatable("message.darkdoppelganger.shadow_altar.end_only"),
                        true
                );
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        if (!level.isClientSide) {
            altar.beginRitual(player, held);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof ShadowAltarBlockEntity altar) {
                altar.dropStoredOrb();
            }
        }
        super.onRemove(state, level, pos, newState, moving);
    }
}
