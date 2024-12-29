package net.bandit.darkdoppelganger.registry;

import net.bandit.darkdoppelganger.DarkDoppelgangerMod;
import net.bandit.darkdoppelganger.items.DoppelgangerRingItem;
import net.bandit.darkdoppelganger.items.SummonScrollItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ItemRegistry {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, DarkDoppelgangerMod.MOD_ID);

    public static final DeferredHolder<Item, Item> SUMMON_SCROLL = ITEMS.register("summon_scroll",
            () -> new SummonScrollItem(new Item.Properties().stacksTo(1)));

    public static final  DeferredHolder<Item, Item> DOPPELGANGER_RING = ITEMS.register("doppelganger_ring",
            () -> new DoppelgangerRingItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
}
