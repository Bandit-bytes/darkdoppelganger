package net.bandit.darkdoppelganger.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.eventbus.api.IEventBus;

import net.bandit.darkdoppelganger.DarkDoppelgangerMod;

public class TabRegistry {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, DarkDoppelgangerMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> DOPPELGANGER_TAB = CREATIVE_MODE_TABS.register("doppelganger_tab", () ->
            CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ItemRegistry.DOPPELGANGER_RING.get()))
                    .title(Component.translatable("itemGroup.darkdoppelganger.doppelganger_tab"))
                    .displayItems((parameters, output) -> {
                        // Add your items here
                        output.accept(ItemRegistry.DOPPELGANGER_RING.get());
                        output.accept(ItemRegistry.SUMMON_SCROLL.get());
                        output.accept(ItemRegistry.SHADOW_ORB.get());
                    })
                    .build()
    );

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
