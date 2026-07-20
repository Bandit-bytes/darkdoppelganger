package net.bandit.darkdoppelganger.registry;

import net.bandit.darkdoppelganger.DarkDoppelgangerMod;
import net.bandit.darkdoppelganger.block.ShadowAltarBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, DarkDoppelgangerMod.MOD_ID);

    public static final DeferredHolder<Block, ShadowAltarBlock> SHADOW_ALTAR =
            BLOCKS.register("shadow_altar", () -> new ShadowAltarBlock(
                    BlockBehaviour.Properties.of()
                            .strength(50.0F, 1200.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.STONE)
                            .lightLevel(state -> 3)
                            .noOcclusion()
            ));

    private ModBlocks() {
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
