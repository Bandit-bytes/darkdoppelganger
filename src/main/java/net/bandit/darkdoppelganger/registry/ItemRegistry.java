package net.bandit.darkdoppelganger.registry;

import net.bandit.darkdoppelganger.DarkDoppelgangerMod;
import net.bandit.darkdoppelganger.item.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ItemRegistry {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, DarkDoppelgangerMod.MOD_ID);

    public static final RegistryObject<Item> SUMMON_SCROLL = ITEMS.register("summon_scroll",
            () -> new SummonScrollItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> SHADOW_ORB = ITEMS.register("shadow_orb",
            () -> new ShadowOrbItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> ELDER_NECKLACE = ITEMS.register("elder_necklace",
            () -> new NecroNecklace(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> SUMMONS_NECKLACE = ITEMS.register("summons_necklace",
            () -> new SummonsNecklace(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> DOPPELGANGER_RING = ITEMS.register("doppelganger_ring",
            () -> new DoppelgangerRingItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

}
