package net.bandit.darkdoppelganger.registry;


import net.bandit.darkdoppelganger.DarkDoppelgangerMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class TabRegistry { public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, DarkDoppelgangerMod.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> DARKDOPPELGANGER_TAB = CREATIVE_MODE_TABS.register("dd_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.item." + DarkDoppelgangerMod.MOD_ID))
            .icon(() -> ItemRegistry.DOPPELGANGER_RING.get().getDefaultInstance())
            .displayItems((parameters, output) -> {

                ItemRegistry.ITEMS.getEntries().forEach(item -> {
                    output.accept(item.get());

                });
            }).build());

    public static void register(net.neoforged.bus.api.IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
