package net.bandit.darkdoppelganger.registry;

import net.bandit.darkdoppelganger.DarkDoppelgangerMod;
import net.bandit.darkdoppelganger.block.ShadowAltarBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, DarkDoppelgangerMod.MOD_ID);

    public static final RegistryObject<Block> SHADOW_ALTAR = BLOCKS.register("shadow_altar",
            () -> new ShadowAltarBlock(BlockBehaviour.Properties.copy(Blocks.CRYING_OBSIDIAN)
                    .strength(50.0F, 1200.0F)
                    .lightLevel(state -> 3)
                    .noOcclusion()));

    private ModBlocks() {
    }
}
