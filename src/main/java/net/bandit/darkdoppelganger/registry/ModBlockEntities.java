package net.bandit.darkdoppelganger.registry;

import net.bandit.darkdoppelganger.DarkDoppelgangerMod;
import net.bandit.darkdoppelganger.block.entity.ShadowAltarBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, DarkDoppelgangerMod.MOD_ID);

    public static final RegistryObject<BlockEntityType<ShadowAltarBlockEntity>> SHADOW_ALTAR =
            BLOCK_ENTITY_TYPES.register("shadow_altar",
                    () -> BlockEntityType.Builder.of(
                            ShadowAltarBlockEntity::new,
                            ModBlocks.SHADOW_ALTAR.get()
                    ).build(null));

    private ModBlockEntities() {
    }
}
