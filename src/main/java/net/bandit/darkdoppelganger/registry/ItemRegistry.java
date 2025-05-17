package net.bandit.darkdoppelganger.registry;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.compat.Curios;
import io.redspace.ironsspellbooks.item.weapons.AttributeContainer;
import net.bandit.darkdoppelganger.DarkDoppelgangerMod;
import net.bandit.darkdoppelganger.items.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
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
            () -> new DoppelgangerRingItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)).withAttributes(
                    Curios.RING_SLOT,
                    new AttributeContainer(AttributeRegistry.COOLDOWN_REDUCTION, 0.20, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                    new AttributeContainer(Attributes.MAX_HEALTH, 20, AttributeModifier.Operation.ADD_VALUE)
            ));

    public static final  DeferredHolder<Item, Item> ELDER_NECKLACE = ITEMS.register("elder_necklace",
            () -> new NecroNecklace(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final  DeferredHolder<Item, Item> SUMMONS_NECKLACE = ITEMS.register("summons_necklace",
            () -> new SummonsNecklace(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    // Method to register sound events
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
