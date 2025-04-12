package net.bandit.darkdoppelganger.registry;

import net.bandit.darkdoppelganger.DarkDoppelgangerMod;
import net.bandit.darkdoppelganger.items.DoppelgangerRingItem;
import net.bandit.darkdoppelganger.items.ShadowOrbItem;
import net.bandit.darkdoppelganger.items.SummonScrollItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ItemRegistry {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, DarkDoppelgangerMod.MOD_ID);

    public static final DeferredHolder<Item, Item> SUMMON_SCROLL = ITEMS.register("summon_scroll",
            () -> new SummonScrollItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, Item> SHADOW_ORB = ITEMS.register("shadow_orb",
            () -> new ShadowOrbItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final  DeferredHolder<Item, Item> DOPPELGANGER_RING = ITEMS.register("doppelganger_ring",
            () -> new DoppelgangerRingItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    // Method to register sound events
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
