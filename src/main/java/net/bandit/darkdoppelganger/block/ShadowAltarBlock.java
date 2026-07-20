package net.bandit.darkdoppelganger.block;

import net.bandit.darkdoppelganger.Config;
import net.bandit.darkdoppelganger.block.entity.ShadowAltarBlockEntity;
import net.bandit.darkdoppelganger.registry.ItemRegistry;
import net.bandit.darkdoppelganger.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class ShadowAltarBlock extends BaseEntityBlock {

    private static final VoxelShape SHAPE = Shapes.or(
            box(0.0, 0.0, 0.0, 16.0, 3.0, 16.0),
            box(3.0, 3.0, 3.0, 13.0, 18.0, 13.0),
            box(1.0, 18.0, 1.0, 15.0, 21.0, 15.0)
    );

    public ShadowAltarBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
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
        return createTickerHelper(type, ModBlockEntities.SHADOW_ALTAR.get(), ShadowAltarBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        ItemStack held = player.getItemInHand(hand);
        if (!held.is(ItemRegistry.SHADOW_ORB.get())) {
            return InteractionResult.PASS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof ShadowAltarBlockEntity altar)) {
            return InteractionResult.PASS;
        }

        if (altar.hasOrb()) {
            if (!level.isClientSide) {
                player.displayClientMessage(
                        Component.translatable("message.darkdoppelganger.shadow_altar.occupied"),
                        true
                );
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (Config.ALTAR_END_ONLY.get() && !level.dimension().equals(Level.END)) {
            if (!level.isClientSide) {
                player.displayClientMessage(
                        Component.translatable("message.darkdoppelganger.shadow_altar.end_only"),
                        true
                );
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (!level.isClientSide) {
            altar.beginRitual(player, held);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof ShadowAltarBlockEntity altar) {
                altar.dropStoredOrb();
            }
        }
        super.onRemove(state, level, pos, newState, moving);
    }
}
